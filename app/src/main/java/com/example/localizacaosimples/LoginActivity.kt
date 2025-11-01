package com.example.localizacaosimples

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: TextInputEditText
    private lateinit var senhaEditText: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var txtCadastro: TextView
    private lateinit var progressBar: ProgressBar

    private val database = FirebaseDatabase.getInstance().getReference("usuarios")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar views
        emailEditText = findViewById(R.id.emailEditText)
        senhaEditText = findViewById(R.id.senhaEditText)
        btnLogin = findViewById(R.id.btnLogin)
        txtCadastro = findViewById(R.id.txtCadastro)
        progressBar = findViewById(R.id.progressBar)

        // Botão de Login
        btnLogin.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val senha = senhaEditText.text.toString().trim()

            if (validarCampos(email, senha)) {
                fazerLogin(email, senha)
            }
        }

        // Link para Cadastro
        txtCadastro.setOnClickListener {
            startActivity(Intent(this, CadastroActivity::class.java))
        }
    }

    private fun validarCampos(email: String, senha: String): Boolean {
        if (email.isEmpty()) {
            emailEditText.error = "Digite o email"
            return false
        }
        if (senha.isEmpty()) {
            senhaEditText.error = "Digite a senha"
            return false
        }
        return true
    }

    private fun fazerLogin(email: String, senha: String) {
        progressBar.visibility = View.VISIBLE
        btnLogin.isEnabled = false

        // Buscar todos os usuários
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var usuarioEncontrado = false

                for (userSnapshot in snapshot.children) {
                    val emailDB = userSnapshot.child("email").getValue(String::class.java)
                    val senhaDB = userSnapshot.child("senha").getValue(String::class.java)

                    if (emailDB == email && senhaDB == senha) {
                        usuarioEncontrado = true
                        val nome = userSnapshot.child("nome").getValue(String::class.java)

                        // Salvar dados do usuário localmente (SharedPreferences)
                        salvarUsuarioLogado(userSnapshot.key!!, nome ?: "Usuário", email)

                        Toast.makeText(this@LoginActivity, "Bem-vindo, $nome!", Toast.LENGTH_SHORT).show()

                        // Ir para MainActivity
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                        break
                    }
                }

                if (!usuarioEncontrado) {
                    Toast.makeText(this@LoginActivity, "Email ou senha incorretos", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LoginActivity, "Erro: ${error.message}", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                btnLogin.isEnabled = true
            }
        })
    }

    private fun salvarUsuarioLogado(userId: String, nome: String, email: String) {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("userId", userId)
            putString("nome", nome)
            putString("email", email)
            putBoolean("isLoggedIn", true)
            apply()
        }
    }
}