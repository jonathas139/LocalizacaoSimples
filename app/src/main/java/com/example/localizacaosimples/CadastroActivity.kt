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

class CadastroActivity : AppCompatActivity() {

    private lateinit var nomeEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var senhaEditText: TextInputEditText
    private lateinit var confirmarSenhaEditText: TextInputEditText
    private lateinit var btnCadastrar: Button
    private lateinit var txtLogin: TextView
    private lateinit var progressBar: ProgressBar

    private val database = FirebaseDatabase.getInstance().getReference("usuarios")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro)

        // Inicializar views
        nomeEditText = findViewById(R.id.nomeEditText)
        emailEditText = findViewById(R.id.emailEditText)
        senhaEditText = findViewById(R.id.senhaEditText)
        confirmarSenhaEditText = findViewById(R.id.confirmarSenhaEditText)
        btnCadastrar = findViewById(R.id.btnCadastrar)
        txtLogin = findViewById(R.id.txtLogin)
        progressBar = findViewById(R.id.progressBar)

        // Botão de Cadastrar
        btnCadastrar.setOnClickListener {
            val nome = nomeEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val senha = senhaEditText.text.toString().trim()
            val confirmarSenha = confirmarSenhaEditText.text.toString().trim()

            if (validarCampos(nome, email, senha, confirmarSenha)) {
                verificarEmailExistente(nome, email, senha)
            }
        }

        // Link para Login
        txtLogin.setOnClickListener {
            finish()
        }
    }

    private fun validarCampos(nome: String, email: String, senha: String, confirmarSenha: String): Boolean {
        if (nome.isEmpty()) {
            nomeEditText.error = "Digite seu nome"
            return false
        }
        if (email.isEmpty()) {
            emailEditText.error = "Digite o email"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Email inválido"
            return false
        }
        if (senha.isEmpty()) {
            senhaEditText.error = "Digite a senha"
            return false
        }
        if (senha.length < 6) {
            senhaEditText.error = "Senha deve ter no mínimo 6 caracteres"
            return false
        }
        if (senha != confirmarSenha) {
            confirmarSenhaEditText.error = "As senhas não coincidem"
            return false
        }
        return true
    }

    private fun verificarEmailExistente(nome: String, email: String, senha: String) {
        progressBar.visibility = View.VISIBLE
        btnCadastrar.isEnabled = false

        // Verificar se o email já existe
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var emailExiste = false

                for (userSnapshot in snapshot.children) {
                    val emailDB = userSnapshot.child("email").getValue(String::class.java)
                    if (emailDB == email) {
                        emailExiste = true
                        break
                    }
                }

                if (emailExiste) {
                    Toast.makeText(this@CadastroActivity, "Email já cadastrado", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    btnCadastrar.isEnabled = true
                } else {
                    cadastrarUsuario(nome, email, senha)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CadastroActivity, "Erro: ${error.message}", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                btnCadastrar.isEnabled = true
            }
        })
    }

    private fun cadastrarUsuario(nome: String, email: String, senha: String) {
        val userId = database.push().key ?: return

        val usuario = mapOf(
            "nome" to nome,
            "email" to email,
            "senha" to senha,
            "dataCadastro" to System.currentTimeMillis()
        )

        database.child(userId).setValue(usuario)
            .addOnSuccessListener {
                Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show()

                // Voltar para tela de login
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao cadastrar: ${e.message}", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                btnCadastrar.isEnabled = true
            }
    }
}