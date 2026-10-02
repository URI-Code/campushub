package br.com.campushub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.campushub.databinding.ActivityCourseListBinding
import br.com.campushub.databinding.ItemCourseBinding
import com.google.firebase.firestore.FirebaseFirestore // banco Firestore
import com.google.firebase.firestore.FirebaseFirestoreException // erro devolvido pelo Firestore

class CourseListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCourseListBinding
    private val firestore = FirebaseFirestore.getInstance() // instancia do Firestore deste app

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCourseListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets()
        binding.recyclerCourses.layoutManager = LinearLayoutManager(this)
    }

    override fun onStart() {
        super.onStart()
        loadCourses()
    }

    private fun loadCourses() {
        binding.progress.visibility = View.VISIBLE
        binding.textEmpty.visibility = View.GONE
        firestore.collection("cursos") // mesma colecao em que o cadastro grava
            .orderBy("nome") // ordena pelo campo nome
            .get() // le todos os documentos dessa colecao
            .addOnCompleteListener(this) { task -> // espera a resposta do Firestore
                binding.progress.visibility = View.GONE
                if (!task.isSuccessful) {
                    Toast.makeText(this, firestoreErrorMessage(task.exception), Toast.LENGTH_LONG).show()
                    return@addOnCompleteListener
                }
                val courses = task.result.documents.map { document ->
                    Course(
                        nome = document.getString("nome").orEmpty(), // campo nome do documento
                        professor = document.getString("professor").orEmpty() // campo professor do documento
                    )
                }
                binding.recyclerCourses.adapter = CourseAdapter(courses)
                binding.textEmpty.visibility = if (courses.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
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

private data class Course(
    val nome: String,
    val professor: String
)

private class CourseAdapter(
    private val courses: List<Course>
) : RecyclerView.Adapter<CourseAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemCourseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCourseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val course = courses[position]
        holder.binding.textName.text = course.nome
        holder.binding.textProfessor.text = holder.itemView.context.getString(
            R.string.professor_line,
            course.professor
        )
    }

    override fun getItemCount(): Int = courses.size
}
