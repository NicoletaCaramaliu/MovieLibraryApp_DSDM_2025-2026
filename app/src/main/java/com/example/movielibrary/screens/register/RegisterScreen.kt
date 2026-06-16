package com.example.movielibrary.screens.register

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.movielibrary.data.local.database.DatabaseProvider
import com.example.movielibrary.data.local.entity.UserEntity
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userDao = DatabaseProvider.getDatabase(context).userDao()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Create Account", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trim() },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                    Toast.makeText(context, "Completează toate câmpurile", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (!email.contains("@") || !email.contains(".")) {
                    Toast.makeText(context, "Email invalid", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (password.length < 8) {
                    Toast.makeText(
                        context,
                        "Parola trebuie să aibă minim 8 caractere",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@Button
                }

                val hasLetter = password.any { it.isLetter() }
                val hasDigit = password.any { it.isDigit() }

                if (!hasLetter || !hasDigit) {
                    Toast.makeText(
                        context,
                        "Parola trebuie să conțină litere și cifre",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@Button
                }

                if (password != confirmPassword) {
                    Toast.makeText(context, "Parolele nu coincid", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                scope.launch {
                    val existingUser = userDao.getUserByEmail(email)

                    if (existingUser != null) {
                        Toast.makeText(context, "User deja existent", Toast.LENGTH_SHORT).show()
                    } else {
                        userDao.insertUser(
                            UserEntity(
                                email = email,
                                password = password
                            )
                        )
                        Toast.makeText(context, "Cont creat cu succes", Toast.LENGTH_SHORT).show()
                        onRegisterSuccess()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register")
        }
    }
}