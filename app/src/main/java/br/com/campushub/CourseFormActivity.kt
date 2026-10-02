package br.com.campushub

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.com.campushub.databinding.ActivityCourseFormBinding
import com.google.firebase.firestore.FirebaseFirestore // banco Firestore
import com.google.firebase.firestore.FirebaseFirestoreException // erro devolvido pelo Firestore

class CourseFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCourseFormBinding
    private val firestore = FirebaseFirestore.getInstance() // instancia do Firestore deste app

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCourseFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets()

        binding.buttonSave.setOnClickListener { saveCourse() }
    }

    private fun saveCourse() {
        val name = binding.inputName.text?.toString()?.trim().orEmpty()
        val professor = binding.inputProfessor.text?.toString()?.trim().orEmpty()
        if (!validate(name, professor)) return

        setLoading(true)
        val course = hashMapOf( // campos que vao para o documento
            "nome" to name,
            "professor" to professor
        )
        firestore.collection("cursos") // colecao onde os cursos ficam
            .add(course) // cria um documento novo; o Firestore gera o id
            .addOnCompleteListener(this) { task -> // espera a resposta do Firestore
                setLoading(false)
                if (task.isSuccessful) {
                    Toast.makeText(this, R.string.course_saved, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, firestoreErrorMessage(task.exception), Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun validate(name: String, professor: String): Boolean {
        binding.nameLayout.error = null
        binding.professorLayout.error = null

        if (name.isEmpty()) {
            binding.nameLayout.error = getString(R.string.error_course_name_required)
            return false
        }
        if (professor.isEmpty()) {
            binding.professorLayout.error = getString(R.string.error_professor_required)
            return false
        }
        return true
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonSave.isEnabled = !loading
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

    private fun firestoreErrorMessage(exception: Exception?): String {
        val code = (exception as? FirebaseFirestoreException)?.code // codigo do erro do Firestore
        return when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> getString(R.string.error_firestore_permission)
            FirebaseFirestoreException.Code.UNAVAILABLE -> getString(R.string.error_network)
            else -> exception?.localizedMessage ?: getString(R.string.error_generic)
        }
    }
}
