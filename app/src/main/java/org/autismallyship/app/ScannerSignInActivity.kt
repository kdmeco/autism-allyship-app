package org.autismallyship.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import org.autismallyship.app.data.Repository
import org.autismallyship.app.databinding.ActivityScannerSignInBinding
import java.util.regex.Pattern

// The only way in is a link given to staff directly. RULES-APP.md is explicit that the scanner is
// reachable only by a deep link, not by any visible control anywhere in the normal app, see the
// intent filter on this activity in AndroidManifest.xml.
class ScannerSignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerSignInBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(AppSettings(this))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityScannerSignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.scannerSignInRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.submitButton.setOnClickListener { submit() }

        // Already signed in from a previous visit. Check the staff flag again rather than
        // trusting the cached session, since an admin may have switched active off since then.
        val existingUser = Firebase.auth.currentUser
        if (existingUser != null) {
            showLoading()
            checkStaffAndContinue(existingUser.uid)
        }
    }

    private fun submit() {
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()

        if (email.isBlank() || !EMAIL_PATTERN.matcher(email).matches()) {
            showError(getString(R.string.scanner_email_required))
            return
        }
        if (password.isBlank()) {
            showError(getString(R.string.scanner_password_required))
            return
        }

        showLoading()
        Firebase.auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    showError(getString(R.string.scanner_sign_in_error))
                } else {
                    checkStaffAndContinue(uid)
                }
            }
            .addOnFailureListener { showError(getString(R.string.scanner_sign_in_error)) }
    }

    // A signed in Firebase user is not necessarily staff, admins sign in through the same
    // FirebaseAuth instance from the website. Access is granted by an active staff document
    // existing, exactly what isActiveStaff() checks server side in the security rules.
    private fun checkStaffAndContinue(uid: String) {
        Repository.loadStaffMember(
            uid,
            onSuccess = { staff ->
                if (staff != null && staff.active) {
                    startActivity(Intent(this, ScannerActivity::class.java))
                    finish()
                } else {
                    Firebase.auth.signOut()
                    showError(getString(R.string.scanner_not_staff))
                }
            },
            // Left signed in here, unlike the not-staff case: this is a connectivity failure, not
            // a rejection, and signing them out would force retyping a password that was correct.
            onError = { showError(getString(R.string.scanner_check_failed)) }
        )
    }

    private fun showLoading() {
        binding.signInError.isVisible = false
        binding.submitButton.isEnabled = false
        binding.loadingRow.isVisible = true
        binding.loadingSpinner.isVisible = !AppSettings(this).isSensoryMode()
    }

    private fun showError(message: String) {
        binding.loadingRow.isVisible = false
        binding.submitButton.isEnabled = true
        binding.signInError.text = message
        binding.signInError.isVisible = true
    }

    private companion object {
        val EMAIL_PATTERN: Pattern = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
