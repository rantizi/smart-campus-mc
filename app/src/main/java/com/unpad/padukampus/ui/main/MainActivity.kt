package com.unpad.padukampus.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.setupWithNavController
import com.unpad.padukampus.PaduKampusApp
import com.unpad.padukampus.R
import com.unpad.padukampus.data.repository.AuthRepository
import com.unpad.padukampus.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                )
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, keyboard.bottom))
            insets
        }
        val auth = (application as PaduKampusApp).container.authRepository
        val host = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = host.navController
        // Gambar 2 Laporan 1: "Sudah login?" menentukan layar awal. Tanpa Firebase (mode lokal)
        // aplikasi langsung ke Beranda.
        val graph = navController.navInflater.inflate(R.navigation.nav_graph)
        graph.setStartDestination(if (perluLogin(auth)) R.id.loginFragment else R.id.homeFragment)
        navController.setGraph(graph, null)
        binding.bottomNavigation.setupWithNavController(navController)
        val tanpaMenuBawah =
            setOf(
                R.id.loginFragment,
                R.id.taskFormFragment,
                R.id.scheduleFormFragment,
                R.id.changeFormFragment,
            )
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.isVisible = destination.id !in tanpaMenuBawah
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                auth.akun.collect { arahkanSesuaiAkun(auth, navController) }
            }
        }
    }

    private fun perluLogin(auth: AuthRepository) = auth.tersedia && auth.akun.value == null

    /** Setelah login ke Beranda; setelah logout back stack dibersihkan (Laporan 1 bagian 5.1). */
    private fun arahkanSesuaiAkun(auth: AuthRepository, navController: NavController) {
        val sekarang = navController.currentDestination?.id ?: return
        val graph = navController.graph
        if (perluLogin(auth) && sekarang != R.id.loginFragment) {
            graph.setStartDestination(R.id.loginFragment)
            navController.navigate(
                R.id.loginFragment,
                null,
                navOptions { popUpTo(graph.id) { inclusive = true } },
            )
        } else if (!perluLogin(auth) && sekarang == R.id.loginFragment) {
            graph.setStartDestination(R.id.homeFragment)
            navController.navigate(
                R.id.homeFragment,
                null,
                navOptions { popUpTo(R.id.loginFragment) { inclusive = true } },
            )
        }
    }
}
