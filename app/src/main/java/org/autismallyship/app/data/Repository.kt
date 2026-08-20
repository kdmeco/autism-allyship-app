package org.autismallyship.app.data

import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore

// Every Firestore read the app makes goes through here, so no screen builds a query of its own and
// the field names from SCHEMA.md appear in one file rather than twenty.
//
// Results come back through onSuccess and onError rather than as suspend functions, because
// Task.await() needs kotlinx-coroutines-play-services and the project does not have it.
//
// Offline is already handled. MainApplication turns on the persistent cache, so these calls answer
// from disk when there is no signal. Nothing here needs a second cache on top of that.
object Repository {

    private val db = Firebase.firestore

    // Events are fetched as one published list and split in code. CONSOLE-STEPS.md records that the
    // events collection deliberately has no composite index, and adding an orderBy to these queries
    // is exactly what would need one. The website splits them the same way.
    fun loadUpcomingEvents(onSuccess: (List<Event>) -> Unit, onError: (Exception) -> Unit) {
        val now = Timestamp.now()
        loadPublishedEvents(
            onSuccess = { events ->
                onSuccess(events.filter { isUpcoming(it, now) }.sortedBy { it.startsAt })
            },
            onError = onError
        )
    }

    fun loadPastEvents(onSuccess: (List<Event>) -> Unit, onError: (Exception) -> Unit) {
        val now = Timestamp.now()
        loadPublishedEvents(
            onSuccess = { events ->
                onSuccess(events.filter { hasPassed(it, now) }.sortedByDescending { it.startsAt })
            },
            onError = onError
        )
    }

    // For the next upcoming event on the home screen.
    fun loadNextEvent(onSuccess: (Event?) -> Unit, onError: (Exception) -> Unit) {
        loadUpcomingEvents(
            onSuccess = { events -> onSuccess(events.firstOrNull()) },
            onError = onError
        )
    }

    // Detail screens take an ID rather than the object itself, so they still work after the system
    // has killed and restored the Activity. A draft is refused by the security rules, so an
    // unpublished ID reaches onError rather than returning null.
    fun loadEvent(eventId: String, onSuccess: (Event?) -> Unit, onError: (Exception) -> Unit) {
        db.collection(EVENTS).document(eventId).get()
            .addOnSuccessListener { document -> onSuccess(document.toObject(Event::class.java)) }
            .addOnFailureListener { error -> onError(error) }
    }

    // The category pills filter this list in code. The posts index that supports the ordering here
    // already exists, and filtering in code keeps it to that one.
    fun loadPosts(onSuccess: (List<Post>) -> Unit, onError: (Exception) -> Unit) {
        db.collection(POSTS)
            .whereEqualTo("published", true)
            .orderBy("publishedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot -> onSuccess(snapshot.toObjects(Post::class.java)) }
            .addOnFailureListener { error -> onError(error) }
    }

    fun loadLatestPost(onSuccess: (Post?) -> Unit, onError: (Exception) -> Unit) {
        db.collection(POSTS)
            .whereEqualTo("published", true)
            .orderBy("publishedAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.toObjects(Post::class.java).firstOrNull())
            }
            .addOnFailureListener { error -> onError(error) }
    }

    // Search, category and province all filter this one list in code. SCHEMA.md settled that,
    // because provinces is an array and filtering on membership in the client keeps the query shape
    // free of it. Sorting by name here rather than in the query avoids a composite index.
    //
    // fromCache is Firestore's own word for "this answer came off the disk, not the server", which
    // is what the offline banner needs. Reading it here rather than checking the connection saves a
    // permission and is more honest, since a live connection can still serve a stale read.
    fun loadResources(
        onSuccess: (resources: List<Resource>, fromCache: Boolean) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection(RESOURCES)
            .whereEqualTo("published", true)
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(
                    snapshot.toObjects(Resource::class.java).sortedBy { it.name },
                    snapshot.metadata.isFromCache
                )
            }
            .addOnFailureListener { error -> onError(error) }
    }

    fun loadResource(
        resourceId: String,
        onSuccess: (Resource?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection(RESOURCES).document(resourceId).get()
            .addOnSuccessListener { document -> onSuccess(document.toObject(Resource::class.java)) }
            .addOnFailureListener { error -> onError(error) }
    }

    fun loadGalleries(onSuccess: (List<Gallery>) -> Unit, onError: (Exception) -> Unit) {
        db.collection(GALLERIES)
            .orderBy("year", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot -> onSuccess(snapshot.toObjects(Gallery::class.java)) }
            .addOnFailureListener { error -> onError(error) }
    }

    fun loadGallery(galleryId: String, onSuccess: (Gallery?) -> Unit, onError: (Exception) -> Unit) {
        db.collection(GALLERIES).document(galleryId).get()
            .addOnSuccessListener { document -> onSuccess(document.toObject(Gallery::class.java)) }
            .addOnFailureListener { error -> onError(error) }
    }

    // Tickets are looked up by document ID, which is the token itself. SCHEMA.md settled this on
    // 20 Aug: the security rules allow get on a known ID but never list on the collection, so a
    // query on the token field would be refused for anyone who is not an admin. onSuccess with
    // null means no ticket matched, which the scanner shows as "not a valid ticket" and a deep
    // link shows as a broken or expired link.
    fun loadTicketByToken(token: String, onSuccess: (Ticket?) -> Unit, onError: (Exception) -> Unit) {
        db.collection(TICKETS).document(token).get()
            .addOnSuccessListener { document -> onSuccess(document.toObject(Ticket::class.java)) }
            .addOnFailureListener { error -> onError(error) }
    }

    // My Tickets reads the tokens it saved locally. Each is its own document get rather than a
    // batched query, for the same reason as above: a whereIn on the token field is still a list
    // operation under the rules, even filtered down to a handful of known IDs. Sorted earliest
    // first, so the screen can cut the list at today.
    fun loadTicketsByTokens(
        tokens: List<String>,
        onSuccess: (List<Ticket>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (tokens.isEmpty()) {
            onSuccess(emptyList())
            return
        }

        val found = mutableListOf<Ticket>()
        var stillWaiting = tokens.size
        var alreadyFailed = false

        for (token in tokens) {
            db.collection(TICKETS).document(token).get()
                .addOnSuccessListener { document ->
                    if (alreadyFailed) return@addOnSuccessListener
                    document.toObject(Ticket::class.java)?.let { found.add(it) }
                    stillWaiting--
                    if (stillWaiting == 0) {
                        onSuccess(found.sortedBy { it.eventStartsAt })
                    }
                }
                .addOnFailureListener { error ->
                    if (alreadyFailed) return@addOnFailureListener
                    alreadyFailed = true
                    onError(error)
                }
        }
    }

    // The scanner calls this after Firebase Auth sign in. A staff member who is not in the
    // collection reads back as null, and one who is there but switched off reads back with active
    // false, which are two different messages to show.
    fun loadStaffMember(uid: String, onSuccess: (StaffMember?) -> Unit, onError: (Exception) -> Unit) {
        db.collection(STAFF).document(uid).get()
            .addOnSuccessListener { document ->
                onSuccess(document.toObject(StaffMember::class.java))
            }
            .addOnFailureListener { error -> onError(error) }
    }

    // The only write the app makes. Tickets and donations are created server side and the security
    // rules refuse both from a client. Fill in createdAt on the Submission before calling this,
    // nothing further along sets it.
    fun sendSubmission(submission: Submission, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        db.collection(SUBMISSIONS).add(submission)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { error -> onError(error) }
    }

    private fun loadPublishedEvents(
        onSuccess: (List<Event>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection(EVENTS)
            .whereEqualTo("published", true)
            .get()
            .addOnSuccessListener { snapshot -> onSuccess(snapshot.toObjects(Event::class.java)) }
            .addOnFailureListener { error -> onError(error) }
    }

    // An event with no startsAt cannot be placed on a timeline, so it appears in neither list rather
    // than being guessed at. SCHEMA.md marks the field required, so this should not happen.
    private fun isUpcoming(event: Event, now: Timestamp): Boolean {
        val startsAt = event.startsAt ?: return false
        return startsAt >= now
    }

    private fun hasPassed(event: Event, now: Timestamp): Boolean {
        val startsAt = event.startsAt ?: return false
        return startsAt < now
    }

    private const val EVENTS = "events"
    private const val POSTS = "posts"
    private const val RESOURCES = "resources"
    private const val GALLERIES = "galleries"
    private const val TICKETS = "tickets"
    private const val STAFF = "staff"
    private const val SUBMISSIONS = "submissions"
}
