package br.com.campushub

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.com.campushub.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth // Authentication do Firebase
import com.google.firebase.auth.FirebaseAuthException // erro devolvido pelo Authentication

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth = FirebaseAuth.getInstance() // instancia do Authentication deste app

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets()

        binding.buttonLogin.setOnClickListener { signIn() }
        binding.buttonCreateAccount.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) { // o aparelho ainda tem uma sessao do Authentication
            openHome()
        }
    }

    private fun signIn() {
        val email = binding.inputEmail.text?.toString()?.trim().orEmpty()
        val password = binding.inputPassword.text?.toString().orEmpty()
        if (!validate(email, password)) return

        setLoading(true)
        auth.signInWithEmailAndPassword(email, password) // entra com e-mail e senha
            .addOnCompleteListener(this) { task -> // espera a resposta do Authentication
                setLoading(false)
                if (task.isSuccessful) {
                    openHome()
                } else {
                    Toast.makeText(this, authErrorMessage(task.exception), Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun validate(email: String, password: String): Boolean {
        binding.emailLayout.error = null
        binding.passwordLayout.error = null

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
        return true
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonLogin.isEnabled = !loading
        binding.buttonCreateAccount.isEnabled = !loading
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
            "ERROR_WRONG_PASSWORD",
            "ERROR_USER_NOT_FOUND",
            "ERROR_INVALID_CREDENTIAL" -> getString(R.string.error_invalid_credentials)

            "ERROR_NETWORK_REQUEST_FAILED" -> getString(R.string.error_network)
            else -> exception?.localizedMessage ?: getString(R.string.error_generic)
        }
    }
}
