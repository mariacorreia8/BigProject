package com.example.bigproject.feature.qr.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import qrcode.QRCode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowQrCodeScreen(
    viewModel: ShowQrCodeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Compartilhar com Enfermeiro") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator()
                    Text(
                        text = "Gerando QR Code...",
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                uiState.error != null -> {
                    Text(
                        text = "Erro: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                uiState.sessionToken != null -> {
                    val qrData =
                        "garcare://patient-session?token=${uiState.sessionToken}"

                    QrCodeImage(
                        content = qrData,
                        modifier = Modifier.size(256.dp)
                    )

                    Text(
                        text = "Peça para o enfermeiro escanear este código",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Gera um ImageBitmap com o QR Code a partir da string [content]
 * usando a lib qrcode-kotlin.
 */
@Composable
private fun QrCodeImage(
    content: String,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(content) {
        generateQrImageBitmap(content)
    }

    Image(
        bitmap = imageBitmap,
        contentDescription = "QR Code",
        modifier = modifier
    )
}

private fun generateQrImageBitmap(content: String): ImageBitmap {
    // Usa o builder de quadrados da lib
    val qrCode = QRCode.ofSquares()
        .withSize(10) // tamanho dos "quadradinhos" do QR
        .build(content)

    val pngBytes: ByteArray = qrCode.renderToBytes()

    // Converte PNG -> Bitmap -> ImageBitmap (Compose)
    val bitmap = BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size)
    return bitmap.asImageBitmap()
}
