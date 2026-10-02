package br.com.campushub

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.com.campushub.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth // Authentication do Firebase
import com.google.firebase.auth.FirebaseAuthException // erro devolvido pelo Authentication

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth = FirebaseAuth.getInstance() // instancia do Authentication deste app

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets()

        binding.buttonRegister.setOnClickListener { register() }
        binding.buttonAlreadyHaveAccount.setOnClickListener { finish() }
    }

    private fun register() {
        val email = binding.inputEmail.text?.toString()?.trim().orEmpty()
        val password = binding.inputPassword.text?.toString().orEmpty()
        val confirmPassword = binding.inputConfirmPassword.text?.toString().orEmpty()
        if (!validate(email, password, confirmPassword)) return

        setLoading(true)
        auth.createUserWithEmailAndPassword(email, password) // cria o usuario no Authentication
            .addOnCompleteListener(this) { task -> // espera a resposta; se der certo, o usuario ja fica logado
                setLoading(false)
                if (task.isSuccessful) {
                    openHome()
                } else {
                    Toast.makeText(this, authErrorMessage(task.exception), Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun validate(email: String, password: String, confirmPassword: String): Boolean {
        binding.emailLayout.error = null
        binding.passwordLayout.error = null
        binding.confirmPasswordLayout.error = null

        if (email.isEmpty()) {
            binding.emailLayout.error = getString(R.string.error_email_required)
            return false
        }
        if (password.isEmpty()) {
            binding.passwordLayout.error = getString(R.string.error_password_required)
            return false
        }
        if (password.length < 6) {
            binding.passwordLayout.error = getString(R.string.error_password_short)
            return false
        }
        if (password != confirmPassword) {
            binding.confirmPasswordLayout.error = getString(R.string.error_password_mismatch)
            return false
        }
        return true
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonRegister.isEnabled = !loading
        binding.buttonAlreadyHaveAccount.isEnabled = !loading
    }

    private fun openHome() {
        startActivity(Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun authErrorMessage(exception: Exception?): String {
        val code = (exception as? FirebaseAuthException)?.errorCode // codigo do erro do Authentication
        return when (code) {
            "ERROR_INVALID_EMAIL" -> getString(R.string.error_invalid_email)
            "ERROR_EMAIL_ALREADY_IN_USE" -> getString(R.string.error_email_in_use)
            "ERROR_WEAK_PASSWORD" -> getString(R.string.error_weak_password)
            "ERROR_NETWORK_REQUEST_FAILED" -> getString(R.string.error_network)
            else -> exception?.localizedMessage ?: getString(R.string.error_generic)
        }
    }
}
