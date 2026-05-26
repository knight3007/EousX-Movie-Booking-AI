package com.uit.eousx.presentation.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.uit.eousx.R
import com.uit.eousx.ui.theme.EousXTheme
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateHome: () -> Unit,
    onNavigateLogin: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkSession()
    }

    SplashContent(
        authState = authState,
        onNavigateHome = onNavigateHome,
        onNavigateLogin = onNavigateLogin
    )
}

@Composable
private fun SplashContent(
    authState: SplashAuthState,
    onNavigateHome: () -> Unit,
    onNavigateLogin: () -> Unit
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.movie_cut))
    var hasNavigated by remember { mutableStateOf(false) }

    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = true,
        iterations = 1
    )

    LaunchedEffect(authState) {
        if (authState != SplashAuthState.Checking && !hasNavigated) {
            delay(900)
            hasNavigated = true
            when (authState) {
                SplashAuthState.Authenticated -> onNavigateHome()
                SplashAuthState.Unauthenticated -> onNavigateLogin()
                SplashAuthState.Checking -> Unit
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_splash),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.Center)
                .offset(x = 30.dp, y = 200.dp)
        )
    }
}

@Preview(name = "Splash", showBackground = true, showSystemUi = true)
@Composable
private fun SplashScreenPreview() {
    EousXTheme {
        SplashContent(
            authState = SplashAuthState.Checking,
            onNavigateHome = {},
            onNavigateLogin = {}
        )
    }
}
