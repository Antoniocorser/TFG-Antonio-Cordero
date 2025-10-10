package Lista.compra

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import Lista.compra.databinding.FragmentAuthEmailBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.Locale

class AuthEmailFragment : Fragment() {
    private lateinit var binding: FragmentAuthEmailBinding
    private var isLogin = true
    private val db = Firebase.firestore
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAuthEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupListeners()
        setupAnimations()
    }

    private fun setupUI() {
        updateAuthModeUI()
    }

    private fun setupListeners() {
        binding.authButton.setOnClickListener {
            authenticateUser()
        }

        binding.authToggle.setOnClickListener {
            toggleAuthMode()
        }

        // Efectos hover similares a React
        setupButtonHoverEffects()
    }

    private fun setupAnimations() {
        // Animación de elevación para el botón principal
        binding.authButton.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !isLoading) {
                animateButtonElevation(4f, 6f)
            } else if (!isLoading) {
                animateButtonElevation(6f, 4f)
            }
        }
    }

    private fun setupButtonHoverEffects() {
        binding.authButton.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    if (!isLoading) {
                        animateButtonPress(true)
                    }
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    if (!isLoading) {
                        animateButtonPress(false)
                    }
                }
            }
            false
        }
    }

    private fun animateButtonPress(isPressed: Boolean) {
        val targetElevation = if (isPressed) 2f else 4f
        val targetTranslationY = if (isPressed) 2f else 0f
        val targetBackgroundColor = if (isPressed) "#2563EB" else "#3B82F6"

        binding.authButton.animate()
            .translationY(targetTranslationY)
            .setDuration(200)
            .start()

        binding.authButton.backgroundTintList =
            android.content.res.ColorStateList.valueOf(Color.parseColor(targetBackgroundColor))

        binding.authButton.elevation = targetElevation
    }

    private fun animateButtonElevation(from: Float, to: Float) {
        val animator = ValueAnimator.ofFloat(from, to)
        animator.addUpdateListener { value ->
            binding.authButton.elevation = value.animatedValue as Float
        }
        animator.duration = 200
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }

    private fun toggleAuthMode() {
        isLogin = !isLogin
        updateAuthModeUI()
        hideError()
    }

    private fun updateAuthModeUI() {
        if (isLogin) {
            binding.authSubtitle.text = "Bienvenido de vuelta"
            binding.authButton.text = "Iniciar Sesión"
            binding.authToggle.text = "¿No tienes cuenta? Regístrate"
            binding.tipText.text = "💡 Consejo: Usa la misma cuenta en todos tus dispositivos para sincronizar tus listas."
        } else {
            binding.authSubtitle.text = "Crea tu cuenta"
            binding.authButton.text = "Crear Cuenta"
            binding.authToggle.text = "¿Ya tienes cuenta? Inicia sesión"
            binding.tipText.text = "💡 Consejo: Crea una contraseña segura para proteger tu cuenta."
        }
    }

    private fun authenticateUser() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        if (!validateInputs(email, password)) {
            return
        }

        setLoadingState(true)
        hideError()

        if (isLogin) {
            signIn(email, password)
        } else {
            register(email, password)
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            showError("Por favor ingresa tu correo electrónico")
            return false
        }

        if (password.isEmpty()) {
            showError("Por favor ingresa tu contraseña")
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Por favor ingresa un correo electrónico válido")
            return false
        }

        return true
    }

    private fun signIn(email: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = Firebase.auth.signInWithEmailAndPassword(email, password).await()
                result.user?.let { user ->
                    updateLastLogin(user.uid, email)
                }
                withContext(Dispatchers.Main) {
                    setLoadingState(false)
                    (activity as? AuthListener)?.onAuthSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setLoadingState(false)
                    showError(getAuthErrorMessage(e))
                }
            }
        }
    }

    private fun register(email: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = Firebase.auth.createUserWithEmailAndPassword(email, password).await()
                result.user?.let { user ->
                    saveUserToFirestore(user.uid, email)
                }
                withContext(Dispatchers.Main) {
                    setLoadingState(false)
                    (activity as? AuthListener)?.onAuthSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setLoadingState(false)
                    showError(getAuthErrorMessage(e))
                }
            }
        }
    }

    private suspend fun saveUserToFirestore(uid: String, email: String) {
        try {
            val userData = hashMapOf(
                "email" to email,
                "uid" to uid,
                "searchEmail" to email.lowercase(Locale.getDefault()),
                "createdAt" to Date(),
                "lastLogin" to Date()
            )

            db.collection("users")
                .document(email)
                .set(userData)
                .await()

            println("Usuario guardado en Firestore correctamente con email como ID: $email")
        } catch (e: Exception) {
            println("Error al guardar usuario en Firestore: ${e.message}")
            throw e
        }
    }

    private suspend fun updateLastLogin(uid: String, email: String) {
        try {
            val updateData = hashMapOf<String, Any>(
                "lastLogin" to Date(),
                "uid" to uid,
                "searchEmail" to email.lowercase(Locale.getDefault())
            )

            db.collection("users")
                .document(email)
                .set(updateData, com.google.firebase.firestore.SetOptions.merge())
                .await()

            println("Último login actualizado para: $email")
        } catch (e: Exception) {
            println("Error al actualizar último login: ${e.message}")
        }
    }

    private fun getAuthErrorMessage(exception: Exception): String {
        return when {
            exception.message?.contains("wrong-password") == true ->
                "Contraseña incorrecta. Por favor, inténtalo de nuevo."
            exception.message?.contains("user-not-found") == true ->
                "No existe una cuenta con este correo electrónico."
            exception.message?.contains("invalid-credential") == true ->
                "Credenciales inválidas. Verifica tu correo y contraseña."
            exception.message?.contains("email-already-in-use") == true ->
                "Este correo ya está registrado. Inicia sesión o usa otro correo."
            exception.message?.contains("weak-password") == true ->
                "La contraseña debe tener al menos 6 caracteres."
            exception.message?.contains("too-many-requests") == true ->
                "Demasiados intentos fallidos. Intenta más tarde o restablece tu contraseña."
            exception.message?.contains("network") == true ->
                "Error de conexión. Verifica tu internet."
            else -> "Error durante la autenticación: ${exception.message}"
        }
    }

    private fun setLoadingState(loading: Boolean) {
        isLoading = loading
        binding.authButton.isEnabled = !loading
        binding.authToggle.isEnabled = !loading
        binding.emailEditText.isEnabled = !loading
        binding.passwordEditText.isEnabled = !loading

        if (loading) {
            binding.authButton.text = if (isLogin) "Iniciando sesión..." else "Creando cuenta..."
            binding.authButton.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor("#9CA3AF"))
            // Deshabilitar efectos durante loading
            binding.authButton.elevation = 2f
            binding.authButton.translationY = 0f
        } else {
            binding.authButton.text = if (isLogin) "Iniciar Sesión" else "Crear Cuenta"
            binding.authButton.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor("#3B82F6"))
            binding.authButton.elevation = 4f
        }
    }

    private fun showError(message: String) {
        binding.errorTextView.text = message
        binding.errorCard.isVisible = true
    }

    private fun hideError() {
        binding.errorCard.isVisible = false
    }

    interface AuthListener {
        fun onAuthSuccess()
    }
}