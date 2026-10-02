package br.com.campushub

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.com.campushub.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth // Authentication do Firebase

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val auth = FirebaseAuth.getInstance() // instancia do Authentication deste app

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets()

        binding.buttonNewCourse.setOnClickListener {
            startActivity(Intent(this, CourseFormActivity::class.java))
        }
        binding.buttonListCourses.setOnClickListener {
            startActivity(Intent(this, CourseListActivity::class.java))
        }
        binding.buttonLogout.setOnClickListener {
            auth.signOut() // encerra a sessao no Authentication
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    override fun onStart() {
        super.onStart()
        val user = auth.currentUser // usuario logado agora; null se ninguem entrou
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            return
        }
        binding.textEmail.text = user.email // e-mail guardado na conta do Authentication
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }
}
