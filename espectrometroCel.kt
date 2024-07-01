package com.example.cameraspectre


import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Rational
import android.view.Surface.ROTATION_0
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.sharp.Face
import androidx.compose.material.icons.sharp.Notifications
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material.icons.sharp.Send
import androidx.compose.material.icons.sharp.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


var espectroThor = listOf<Float>(.04f, .58f)
var bandaVisivel = listOf<Float>(400f, 700f)


private fun modulo(x: Float): Float {
	if (x < 0){ return -x }
	return x
}
private fun modulo(x: Int): Int {
	if (x < 0){ return -x }
	return x
}

private fun calcIntensidade(pixel: Int): Float {
	val kr = 1f
	val kg = 1f
	val kb = 1f
	return pixel.red * kr + pixel.green * kg + pixel.blue * kb
}

private fun normalizar(vet: List<Float>): List<Float> {
	var max = 0f
	for (x in vet){
		if (max<x){ max = x }
	}
	val vetTemp = mutableListOf<Float>()
	for (x in vet){
		vetTemp.add(x/max)
	}
	return vetTemp
}

private fun mensurarSpectre(
	uriSpectre: Uri?,
	corretor: List<Float>,
	sensibilidade: Int,
	Espessura: Int,
	orientacaoPre: Int, // 1=horz; 2=vert; outro=auto
	posicaoUser: Int?,
	context: Context
): List<List<Float>> {
	val limiar = sensibilidade
	val espessura = Espessura
	var posEsquerda = 10
	var posDireita = 20
	var posTopo = 10
	var posFundo = 20
	var tamLinha = 10
	var tamLinhaPerpendic = 10
	var orientacao = orientacaoPre
	var uriTemp: Uri
	val rest = mutableListOf(mutableListOf<Float>(), mutableListOf<Float>())
	var posicaoCentro = 10
	var posicaoCentroTemp = posicaoUser


	if (uriSpectre != null){
		uriTemp = null ?: uriSpectre

		val btmTemp = if (Build.VERSION.SDK_INT < 28) {
			MediaStore.Images
				.Media.getBitmap(context.contentResolver,uriTemp)
		} else {
			val source = ImageDecoder.createSource(context.contentResolver,uriTemp)
			ImageDecoder.decodeBitmap(source)
		}
		val imgTemp = btmTemp.copy(Bitmap.Config.ARGB_8888, true)
		btmTemp.recycle()

		when (orientacao){
			1 -> { // HORIZONTAL
				tamLinha = imgTemp.width
				tamLinhaPerpendic = imgTemp.height }
			2 ->{ // VERTICAL
				tamLinha = imgTemp.height
				tamLinhaPerpendic = imgTemp.width }
			else -> { // AUTO
				var posVertTemp = (imgTemp.height/2).toInt()
				var i1 = 1
				posEsquerda = imgTemp.width/2.toInt()
				while (i1 < imgTemp.height-2){
					for (j in 1 until imgTemp.width){
						if (calcIntensidade(imgTemp.getPixel(j, i1)) > limiar){
							posEsquerda = j
							posVertTemp = i1
							i1 = imgTemp.height
							break
						}
					}
					i1 += 3
				}
				posDireita = posEsquerda + 10
				for (i in imgTemp.width - 2 downTo posEsquerda + 1){
					if (calcIntensidade(imgTemp.getPixel(i, posVertTemp)) > limiar){
						posDireita = i
						break
					}
				}
				var ptoMedio = ((posDireita + posEsquerda)/2).toInt()
				posTopo = imgTemp.height/2 - 5
				for(i in 1 until imgTemp.height){
					if (calcIntensidade(imgTemp.getPixel(ptoMedio, i)) > limiar){
						posTopo = i
						break
					}
				}
				posFundo = posTopo + 10
				for(i in imgTemp.height-2 downTo posTopo + 1){
					if (calcIntensidade(imgTemp.getPixel(ptoMedio, i)) > limiar){
						posFundo = i
						break
					}
				}
				ptoMedio = ((posTopo + posFundo)/2).toInt()
				posEsquerda = imgTemp.width/2 - 5
				for(i in 1 until imgTemp.width){
					if (calcIntensidade(imgTemp.getPixel(i, ptoMedio)) > limiar){
						posEsquerda = i
						break
					}
				}
				posDireita = posEsquerda + 10
				for(i in imgTemp.width-2 downTo posEsquerda + 1){
					if (calcIntensidade(imgTemp.getPixel(i, ptoMedio)) > limiar){
						posDireita = i
						break
					}
				}

				if (modulo(posFundo - posTopo) > modulo(posDireita - posEsquerda)){ // VERTICAL
					tamLinha = imgTemp.height
					tamLinhaPerpendic = imgTemp.width
					posicaoCentroTemp = modulo((posDireita + posEsquerda)/2).toInt()
					orientacao = 2
				}else { // HORIZONTAL
					tamLinha = imgTemp.width
					tamLinhaPerpendic = imgTemp.height
					posicaoCentroTemp = modulo((posFundo + posTopo)/2).toInt()
					orientacao = 1
				}
			}
		}

		if (posicaoCentroTemp != null && posicaoCentroTemp > 0 && posicaoCentroTemp < tamLinhaPerpendic){
			posicaoCentro = modulo(posicaoCentroTemp)
		} else {
			var posTempAnter = (tamLinhaPerpendic/2).toInt() - 5
			var posCentroTempPerpendc = (tamLinhaPerpendic/2).toInt()
			var i2 = 1
			while (i2 < tamLinha){
				for (cont5 in 1 until tamLinhaPerpendic) {
					val intensidadeTemp = if (orientacao == 1) { // HORIZONTAL
						calcIntensidade(imgTemp.getPixel(i2, cont5))
					} else { // VERTICAL
						calcIntensidade(imgTemp.getPixel(cont5, i2))
					}
					if (intensidadeTemp > limiar) {
						posTempAnter = cont5
						posCentroTempPerpendc = i2
						i2 = tamLinha
						break
					}
				}
				i2 += 3
			}
			var posTempPost = posTempAnter + 10
			for (cont6 in tamLinhaPerpendic - 1 downTo posTempAnter + 1 ){
				val intensidadeTemp = if (orientacao == 1) { // HORIZONTAL
					calcIntensidade(imgTemp.getPixel(posCentroTempPerpendc, cont6))
				} else { // VERTICAL
					calcIntensidade(imgTemp.getPixel(cont6, posCentroTempPerpendc))
				}
				if (intensidadeTemp > limiar){
					posTempPost = cont6
					break
				}
			}
			val posTempCentro = ((posTempPost + posTempAnter)/2).toInt()
			var posPerpendicAnter = (tamLinha/2).toInt()
			for (cont7 in 1 until tamLinha){
				val intensidadeTemp = if (orientacao == 1) { // HORIZONTAL
					calcIntensidade(imgTemp.getPixel(cont7, posTempCentro))
				} else { // VERTICAL
					calcIntensidade(imgTemp.getPixel(posTempCentro, cont7))
				}
				if (intensidadeTemp > limiar){
					posPerpendicAnter = cont7
					break
				}
			}
			var posPerpendicPost = (tamLinha/2).toInt()
			for (cont8 in tamLinha - 1 downTo  posPerpendicAnter + 1){
				val intensidadeTemp = if (orientacao == 1) { // HORIZONTAL
					calcIntensidade(imgTemp.getPixel(cont8, posTempCentro))
				} else { // VERTICAL
					calcIntensidade(imgTemp.getPixel(posTempCentro, cont8))
				}
				if (intensidadeTemp > limiar){
					posPerpendicPost = cont8
					break
				}
			}
			posCentroTempPerpendc = ((posPerpendicAnter + posPerpendicPost)/2).toInt()
			for (cont9 in tamLinhaPerpendic - 1 downTo posTempAnter + 1 ){
				val intensidadeTemp = if (orientacao == 1) { // HORIZONTAL
					calcIntensidade(imgTemp.getPixel(posCentroTempPerpendc, cont9))
				} else { // VERTICAL
					calcIntensidade(imgTemp.getPixel(cont9, posCentroTempPerpendc))
				}
				if (intensidadeTemp > limiar){
					posTempPost = cont9
					break
				}
			}
			for (cont1 in tamLinhaPerpendic - 1 downTo posTempAnter + 1 ){
				val intensidadeTemp = if (orientacao == 1) { // HORIZONTAL
					calcIntensidade(imgTemp.getPixel(posCentroTempPerpendc, cont1))
				} else { // VERTICAL
					calcIntensidade(imgTemp.getPixel(cont1, posCentroTempPerpendc))
				}
				if (intensidadeTemp > limiar){
					posTempPost = cont1
					break
				}
			}
			posicaoCentro = ((posTempPost + posTempAnter)/2).toInt()
		}

		val espessuraAnter = if (posicaoCentro - espessura < 0){
			posicaoCentro - 1
		}else{
			espessura
		}
		val espessuraPoster = if (posicaoCentro + espessura > tamLinha - 1 ){
			tamLinha - posicaoCentro - 1
		}else{
			espessura
		}
		var valTemp: Float

		for (i in 0 until tamLinha){
			valTemp = 0f
			for(j in 0 until espessuraAnter){
				valTemp += if (orientacao == 1) { // HORIZONTAL
					calcIntensidade(imgTemp.getPixel(i, posicaoCentro - j))
				} else { // VERTICAL
					calcIntensidade(imgTemp.getPixel(posicaoCentro - j, i))
				}
			}
			for(j in 0 until espessuraPoster){
				valTemp += if (orientacao == 1) { // HORIZONTAL
					calcIntensidade(imgTemp.getPixel(i, posicaoCentro + j))
				} else { // VERTICAL
					calcIntensidade(imgTemp.getPixel(posicaoCentro + j, i))
				}
			}
			valTemp /= (espessuraAnter + espessuraPoster + 1)
			rest[1].add(valTemp)
		}

		if (corretor[0] == 1f && corretor.size-1 == rest[1].size) { // APLICANDO CORRETOR
			for (cont2 in 0 until rest[1].size){
				rest[1][cont2] = corretor[cont2 + 1] * rest[1][cont2]
				rest[0].add(bandaVisivel[cont2])
			}
		} else { // SEM CORRETOR OU CORRETOR ERRADO
			for (cont3 in 0 until rest[1].size){
				rest[0].add(cont3.toFloat()+300f)
			}
		}

		var max = 0.1f
		for (x in rest[1]){
			if (max<x){ max = x }
		}
		for (i in 0 until rest[1].size){
			rest[1][i] /= max
		}

		return rest

	} else {
		return listOf(listOf<Float>(0.11F, 0.22F, 0.33F), listOf<Float>(0.11F, 0.22F, 0.33F))
	}

}


private fun mensurarSpectre2(
	uriSpectre: Uri?,
	corretor: List<Float>,
	posicao: Int,
	pixelInicial: Int,
	pixelFinal: Int,
	Orientacao: Int, // 1=horz; 2=vert; outro=auto
	reverso: Boolean,
	Espessura: Int,
	context: Context
): List<List<Float>> {
	val espessura = Espessura
	val orientacao = Orientacao
	val uriTemp: Uri
	val posicaoCentro = posicao

	/* espessura < dist(posicao; borda paralela)
	orientacao -> 1=horz; 2=vert; outro=auto */

	if (uriSpectre != null){
		uriTemp = null ?: uriSpectre

		val btmTemp = if (Build.VERSION.SDK_INT < 28) {
			MediaStore.Images
				.Media.getBitmap(context.contentResolver,uriTemp)
		} else {
			val source = ImageDecoder.createSource(context.contentResolver,uriTemp)
			ImageDecoder.decodeBitmap(source)
		}
		val imgTemp = btmTemp.copy(Bitmap.Config.ARGB_8888, true)
		btmTemp.recycle()

		// computar vet intensidades
		var valTemp: Float
		val vetIntensidades = mutableListOf<Float>()
		if (reverso){
			for (i in pixelFinal.toInt() downTo pixelInicial.toInt()) {
				if (orientacao == 1) { // HORIZONTAL
					valTemp = calcIntensidade(imgTemp.getPixel(i, posicaoCentro))
					for (j in 1..(espessura / 2).toInt()) {
						valTemp += calcIntensidade(imgTemp.getPixel(i, posicaoCentro - j))
						valTemp += calcIntensidade(imgTemp.getPixel(i, posicaoCentro + j))
					}
				} else { // VERTICAL
					valTemp = calcIntensidade(imgTemp.getPixel(posicaoCentro, i))
					for (j in 1..(espessura / 2).toInt()) {
						valTemp += calcIntensidade(imgTemp.getPixel(posicaoCentro - j, i))
						valTemp += calcIntensidade(imgTemp.getPixel(posicaoCentro + j, i))
					}
				}
				vetIntensidades.add(valTemp / espessura)
			}
		} else {
			for (i in pixelInicial.toInt()until pixelFinal.toInt()){
				if (orientacao == 1) { // HORIZONTAL
					valTemp = calcIntensidade(imgTemp.getPixel(i, posicaoCentro))
					for (j in 1..(espessura / 2).toInt()) {
						valTemp += calcIntensidade(imgTemp.getPixel(i, posicaoCentro - j))
						valTemp += calcIntensidade(imgTemp.getPixel(i, posicaoCentro + j))
					}
				} else { // VERTICAL
					valTemp = calcIntensidade(imgTemp.getPixel(posicaoCentro, i))
					for (j in 1..(espessura / 2).toInt()) {
						valTemp += calcIntensidade(imgTemp.getPixel(posicaoCentro - j, i))
						valTemp += calcIntensidade(imgTemp.getPixel(posicaoCentro + j, i))
					}
				}
				vetIntensidades.add(valTemp / espessura)
			}
		}

		// usar corretor
		val vetICorrigida = mutableListOf<Float>()
		for (i in 0 until vetIntensidades.size){
			vetICorrigida.add(vetIntensidades[i] * corretor[i])
		}

		// RETORNO
		return listOf(listOf(1f, 2f), normalizar(vetICorrigida))
	} else {
		return listOf(listOf<Float>(0.55f, 0.22f, 0.55f), listOf<Float>(0.11f, 0.22f, 0.33f))
	}
}


private fun Calibrar(
	uriBranco: Uri,
	uriLamb1: Uri,
	uriLamb2: Uri,
	lamb1: Float,
	lamb2: Float,
	vetThor: List<List<Float>>,
	sensibilidade: Int,
	Espessura: Int,
	orientacaoPre: Int, // 1=horz; 2=vert; outro=auto
	posicaoUser: Int?,
	context: Context
): MutableList<Any> {
	val limiar = sensibilidade
	val espessura = Espessura
	var posEsquerda = 10
	var posDireita = 20
	var posTopo = 10
	var posFundo = 20
	var tamLinha = 10
	var tamLinhaPerpendic = 10
	var orientacao = orientacaoPre
	var uriTemp: Uri
	val rest = mutableListOf(mutableListOf<Float>(), mutableListOf<Float>(), Int, Int, Int, Int, Int, Boolean)
	var posicaoCentro = 10
	var posicaoCentroTemp = posicaoUser


	uriTemp = null ?: uriBranco

	val btmTemp = if (Build.VERSION.SDK_INT < 28) {
		MediaStore.Images
			.Media.getBitmap(context.contentResolver,uriTemp)
	} else {
		val source = ImageDecoder.createSource(context.contentResolver,uriTemp)
		ImageDecoder.decodeBitmap(source)
	}
	val imgTemp = btmTemp.copy(Bitmap.Config.ARGB_8888, true)
	btmTemp.recycle()

	when (orientacao){
		1 -> { // HORIZONTAL
			tamLinha = imgTemp.width
			tamLinhaPerpendic = imgTemp.height }
		2 ->{ // VERTICAL
			tamLinha = imgTemp.height
			tamLinhaPerpendic = imgTemp.width }
		else -> { // AUTO
			var posVertTemp = (imgTemp.height/2).toInt()
			var i1 = 1
			posEsquerda = imgTemp.width/2.toInt()
			while (i1 < imgTemp.height-2){
				for (j in 1 until imgTemp.width){
					if (calcIntensidade(imgTemp.getPixel(j, i1)) > limiar){
						posEsquerda = j
						posVertTemp = i1
						i1 = imgTemp.height
						break
					}
				}
				i1 += 3
			}
			posDireita = posEsquerda + 10
			for (i in imgTemp.width - 2 downTo posEsquerda + 1){
				if (calcIntensidade(imgTemp.getPixel(i, posVertTemp)) > limiar){
					posDireita = i
					break
				}
			}
			var ptoMedio = ((posDireita + posEsquerda)/2).toInt()
			posTopo = imgTemp.height/2 - 5
			for(i in 1 until imgTemp.height){
				if (calcIntensidade(imgTemp.getPixel(ptoMedio, i)) > limiar){
					posTopo = i
					break
				}
			}
			posFundo = posTopo + 10
			for(i in imgTemp.height-2 downTo posTopo + 1){
				if (calcIntensidade(imgTemp.getPixel(ptoMedio, i)) > limiar){
					posFundo = i
					break
				}
			}
			ptoMedio = ((posTopo + posFundo)/2).toInt()
			posEsquerda = imgTemp.width/2 - 5
			for(i in 1 until imgTemp.width){
				if (calcIntensidade(imgTemp.getPixel(i, ptoMedio)) > limiar){
					posEsquerda = i
					break
				}
			}
			posDireita = posEsquerda + 10
			for(i in imgTemp.width-2 downTo posEsquerda + 1){
				if (calcIntensidade(imgTemp.getPixel(i, ptoMedio)) > limiar){
					posDireita = i
					break
				}
			}

			if (modulo(posFundo - posTopo) > modulo(posDireita - posEsquerda)){ // VERTICAL
				tamLinha = imgTemp.height
				tamLinhaPerpendic = imgTemp.width
				posicaoCentroTemp = modulo((posDireita + posEsquerda)/2).toInt()
				orientacao = 2
			}else { // HORIZONTAL
				tamLinha = imgTemp.width
				tamLinhaPerpendic = imgTemp.height
				posicaoCentroTemp = modulo((posFundo + posTopo)/2).toInt()
				orientacao = 1
			}
		}
	}

	if (posicaoCentroTemp != null && posicaoCentroTemp > 0 && posicaoCentroTemp < tamLinhaPerpendic){
		posicaoCentro = modulo(posicaoCentroTemp)
	} else {
		var posTempAnter = (tamLinhaPerpendic/2).toInt() - 5
		var posCentroTempPerpendc = (tamLinhaPerpendic/2).toInt()
		var i2 = 1
		while (i2 < tamLinha){
			for (cont5 in 1 until tamLinhaPerpendic) {
				val intensidadeTemp = if (orientacao == 1) { // HORIZONTAL
					calcIntensidade(imgTemp.getPixel(i2, cont5))
				} else { // VERTICAL
					calcIntensidade(imgTemp.getPixel(cont5, i2))
				}
				if (intensidadeTemp > limiar) {
					posTempAnter = cont5
					posCentroTempPerpendc = i2
					i2 = tamLinha
					break
				}
			}
			i2 += 3
		}
		var posTempPost = posTempAnter + 10
		for (cont6 in tamLinhaPerpendic - 1 downTo posTempAnter + 1 ){
			val intensidadeTemp = if (orientacao == 1) { // HORIZONTAL
				calcIntensidade(imgTemp.getPixel(posCentroTempPerpendc, cont6))
			} else { // VERTICAL
				calcIntensidade(imgTemp.getPixel(cont6, posCentroTempPerpendc))
			}
			if (intensidadeTemp > limiar){
				posTempPost = cont6
				break
			}
		}
		val posTempCentro = ((posTempPost + posTempAnter)/2).toInt()
		var posPerpendicAnter = (tamLinha/2).toInt()
		for (cont7 in 1 until tamLinha){
			val intensidadeTemp = if (orientacao == 1) { // HORIZONTAL
				calcIntensidade(imgTemp.getPixel(cont7, posTempCentro))
			} else { // VERTICAL
				calcIntensidade(imgTemp.getPixel(posTempCentro, cont7))
			}
			if (intensidadeTemp > limiar){
				posPerpendicAnter = cont7
				break
			}
		}
		var posPerpendicPost = (tamLinha/2).toInt()
		for (cont8 in tamLinha - 1 downTo  posPerpendicAnter + 1){
			val intensidadeTemp = if (orientacao == 1) { // HORIZONTAL
				calcIntensidade(imgTemp.getPixel(cont8, posTempCentro))
			} else { // VERTICAL
				calcIntensidade(imgTemp.getPixel(posTempCentro, cont8))
			}
			if (intensidadeTemp > limiar){
				posPerpendicPost = cont8
				break
			}
		}
		posCentroTempPerpendc = ((posPerpendicAnter + posPerpendicPost)/2).toInt()
		for (cont9 in tamLinhaPerpendic - 1 downTo posTempAnter + 1 ){
			val intensidadeTemp = if (orientacao == 1) { // HORIZONTAL
				calcIntensidade(imgTemp.getPixel(posCentroTempPerpendc, cont9))
			} else { // VERTICAL
				calcIntensidade(imgTemp.getPixel(cont9, posCentroTempPerpendc))
			}
			if (intensidadeTemp > limiar){
				posTempPost = cont9
				break
			}
		}
		for (cont1 in tamLinhaPerpendic - 1 downTo posTempAnter + 1 ){
			val intensidadeTemp = if (orientacao == 1) { // HORIZONTAL
				calcIntensidade(imgTemp.getPixel(posCentroTempPerpendc, cont1))
			} else { // VERTICAL
				calcIntensidade(imgTemp.getPixel(cont1, posCentroTempPerpendc))
			}
			if (intensidadeTemp > limiar){
				posTempPost = cont1
				break
			}
		}
		posicaoCentro = ((posTempPost + posTempAnter)/2).toInt()
	}

	val espessuraAnter = if (posicaoCentro - espessura < 0){
		posicaoCentro - 1
	}else{
		espessura
	}
	val espessuraPoster = if (posicaoCentro + espessura > tamLinha - 1 ){
		tamLinha - posicaoCentro - 1
	}else{
		espessura
	}

	var valTemp: Float

	uriTemp = null ?: uriLamb1
	val btmTemp1 = if (Build.VERSION.SDK_INT < 28) {
		MediaStore.Images
			.Media.getBitmap(context.contentResolver,uriTemp)
	} else {
		val source = ImageDecoder.createSource(context.contentResolver,uriTemp)
		ImageDecoder.decodeBitmap(source)
	}
	val imgTempL1 = btmTemp1.copy(Bitmap.Config.ARGB_8888, true)
	btmTemp1.recycle()

	var posLamb1Esqr = 0
	for (i in 0 until tamLinha){
		valTemp = if(orientacao == 1) {
			calcIntensidade(imgTempL1.getPixel(i, posicaoCentro))
		} else {
			calcIntensidade(imgTempL1.getPixel(posicaoCentro, i))
		}
		if  (valTemp > limiar){
			posLamb1Esqr = i
			break
		}
	}
	var posLamb1Dir = 0
	for (i in tamLinha - 1 downTo posLamb1Esqr) {
		valTemp = if(orientacao == 1) {
			calcIntensidade(imgTempL1.getPixel(i, posicaoCentro))
		} else {
			calcIntensidade(imgTempL1.getPixel(posicaoCentro, i))
		}
		if  (valTemp > limiar){
			posLamb1Dir = i
			break
		}
	}
	val posLamb1 = (posLamb1Esqr + posLamb1Dir)/2

	uriTemp = null ?: uriLamb2
	val btmTemp2 = if (Build.VERSION.SDK_INT < 28) {
		MediaStore.Images
			.Media.getBitmap(context.contentResolver,uriTemp)
	} else {
		val source = ImageDecoder.createSource(context.contentResolver,uriTemp)
		ImageDecoder.decodeBitmap(source)
	}
	val imgTempL2 = btmTemp2.copy(Bitmap.Config.ARGB_8888, true)
	btmTemp2.recycle()

	var posLamb2Esqr = 0
	for (i in 0 until tamLinha){
		valTemp = if(orientacao == 1) {
			calcIntensidade(imgTempL2.getPixel(i, posicaoCentro))
		} else {
			calcIntensidade(imgTempL2.getPixel(posicaoCentro, i))
		}
		if  (valTemp > limiar){
			posLamb2Esqr = i
			break
		}
	}
	var posLamb2Dir = 0
	for (i in tamLinha - 1 downTo posLamb2Esqr) {
		valTemp = if(orientacao == 1) {
			calcIntensidade(imgTempL2.getPixel(i, posicaoCentro))
		} else {
			calcIntensidade(imgTempL2.getPixel(posicaoCentro, i))
		}
		if  (valTemp > limiar){
			posLamb2Dir = i
			break
		}
	}
	val posLamb2 = (posLamb2Esqr + posLamb2Dir)/2

	val passo = modulo(lamb2 - lamb1)/modulo(posLamb2 - posLamb1)

	val reverso: Boolean
	val pixelInicial: Float
	val pixelFinal: Float
	if (posLamb1 < posLamb2){
		pixelInicial =  posLamb1 - (lamb1 - 400)/passo
		pixelFinal =  posLamb2 + (700 - lamb2)/passo
		reverso = false
	} else {
		pixelInicial = posLamb1 + (lamb1 - 400)/passo
		pixelFinal = posLamb2 - (700 - lamb2)/passo
		reverso = true
	}

	val lambInicial = if (reverso){
		lamb1 + modulo(posLamb1 - pixelInicial) * passo
	}else{
		lamb1 - modulo(posLamb1 - pixelInicial) * passo
	}
	val vetLambdas = mutableListOf<Float>()
	for (i in 0 .. (pixelFinal - pixelInicial).toInt()){
		vetLambdas.add(lambInicial + i * passo)
	}

	val vetIntensidades = mutableListOf<Float>()
	if (reverso){
		for (i in pixelFinal.toInt() downTo pixelInicial.toInt()){
			valTemp = if (orientacao == 1) { // HORIZONTAL
				calcIntensidade(imgTemp.getPixel(i, posicaoCentro))
			} else { // VERTICAL
				calcIntensidade(imgTemp.getPixel(posicaoCentro, i))
			}
			vetIntensidades.add(valTemp)
		}
	} else {
		for (i in pixelInicial.toInt() until pixelFinal.toInt()){
			valTemp = if (orientacao == 1) { // HORIZONTAL
				calcIntensidade(imgTemp.getPixel(i, posicaoCentro))
			} else { // VERTICAL
				calcIntensidade(imgTemp.getPixel(posicaoCentro, i))
			}
			vetIntensidades.add(valTemp)
		}
	}

	val vetIntensidadesThor = mutableListOf<Float>()
	var tempPrevio = 0
	for (i in 0 until vetLambdas.size){
		var tempIntensidade = 0.5f
		for (j in tempPrevio until vetThor[0].size-1) {
			if (vetLambdas[i] == vetThor[0][j]) {
				tempIntensidade = vetThor[1][j]
				tempPrevio = 0
				break
			} else if (vetLambdas[i] > vetThor[0][j] && vetLambdas[i] < vetThor[0][j + 1]) {
				tempIntensidade = (vetThor[1][j] + vetThor[1][j + 1]) / 2
				tempPrevio = 0
				break
			}
		}
		vetIntensidadesThor.add(tempIntensidade)
	}

	val vetCorretor = mutableListOf<Float>()
	for (i in 0 until vetIntensidades.size){
		if (vetIntensidades[i] != 0f) {
			vetCorretor.add(vetIntensidadesThor[i] / vetIntensidades[i])
		} else {
			vetCorretor.add(0f)
		}
	}


	// RETORNO
	rest[0] = vetLambdas
	rest[1] = vetCorretor
	rest[2] = pixelInicial
	rest[3] = pixelFinal
	rest[4] = posicaoCentro
	rest[5] = orientacao
	rest[6] = reverso
	rest[7] = true

	return rest
}


@Composable
fun CameraView(onImageCaptured: (Uri, Boolean) -> Unit, onError: (ImageCaptureException) -> Unit) {
	val context = LocalContext.current
	var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
	val imageCapture: ImageCapture = remember {
		ImageCapture.Builder().build()
	}

	var uriSpectreArqv by remember {
		mutableStateOf<Uri?>(null)
	}
	var uriBrancoArqv by remember {
		mutableStateOf<Uri?>(null)
	}
	var uriLamb1Arqv by remember {
		mutableStateOf<Uri?>(null)
	}
	var uriLamb2Arqv by remember {
		mutableStateOf<Uri?>(null)
	}
	var uriSpectreCam by remember {
		mutableStateOf<Uri?>(null)
	}
	var uriBrancoCam by remember {
		mutableStateOf<Uri?>(null)
	}
	var uriLamb1Cam by remember {
		mutableStateOf<Uri?>(null)
	}
	var uriLamb2Cam by remember {
		mutableStateOf<Uri?>(null)
	}
	val lancador = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.GetContent()) {
			uri: Uri? -> uriSpectreArqv = uri
	}
	val lancador2 = rememberLauncherForActivityResult(contract =
	ActivityResultContracts.GetContent()) { uri: Uri? ->
		uriBrancoArqv = uri
	}
	val lancador3 = rememberLauncherForActivityResult(contract =
	ActivityResultContracts.GetContent()) { uri: Uri? ->
		uriLamb1Arqv = uri
	}
	val lancador4 = rememberLauncherForActivityResult(contract =
	ActivityResultContracts.GetContent()) { uri: Uri? ->
		uriLamb2Arqv = uri
	}


	CameraPreviewView(
		uriBrancoArqv,
		uriLamb1Arqv,
		uriLamb2Arqv,
		uriSpectreArqv,
		uriBrancoCam,
		uriLamb1Cam,
		uriLamb2Cam,
		uriSpectreCam,
		imageCapture,
		lensFacing,
		cameraUIAction = {
			when (it) {
				is CameraUIAction.OnCameraClickBranco -> {
					imageCapture.takePicture(
						context,
						lensFacing,
						{ uri, fromGallery ->
							Log.d(ContentValues.TAG, "Image Uri Captured from Camera View")
							uriBrancoCam = uri
						},
						onError
					)
					return@CameraPreviewView true
				}
				is CameraUIAction.OnCameraClickLamb1 -> {
					imageCapture.takePicture(
						context,
						lensFacing,
						{ uri, fromGallery ->
							Log.d(ContentValues.TAG, "Image Uri Captured from Camera View")
							uriLamb1Cam = uri
						},
						onError
					)
					return@CameraPreviewView true
				}
				is CameraUIAction.OnCameraClickLamb2 -> {
					imageCapture.takePicture(
						context,
						lensFacing,
						{ uri, fromGallery ->
							Log.d(ContentValues.TAG, "Image Uri Captured from Camera View")
							uriLamb2Cam = uri
						},
						onError
					)
					return@CameraPreviewView true
				}
				is CameraUIAction.OnCameraClickSpectre -> {
					imageCapture.takePicture(
						context,
						lensFacing,
						{ uri, fromGallery ->
							Log.d(ContentValues.TAG, "Image Uri Captured from Camera View")
							uriSpectreCam = uri
						},
						onError
					)
				}
				is CameraUIAction.OnSwitchCameraClick -> {
					lensFacing =
						if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT
						else
							CameraSelector.LENS_FACING_BACK
				}
				is CameraUIAction.OnGalleryViewClick -> {
					if (true == context.getOutputDirectory().listFiles()?.isNotEmpty()) {
						lancador.launch("image/*")
					}
				}
				is CameraUIAction.OnGalleryViewBrancoClick -> {
					if (true == context.getOutputDirectory().listFiles()?.isNotEmpty()) {
						lancador2.launch("image/*")
					}
				}
				is CameraUIAction.OnGalleryViewLamb1Click -> {
					if (true == context.getOutputDirectory().listFiles()?.isNotEmpty()) {
						lancador3.launch("image/*")
					}
				}
				is CameraUIAction.OnGalleryViewLamb2Click -> {
					if (true == context.getOutputDirectory().listFiles()?.isNotEmpty()) {
						lancador4.launch("image/*")
					}
				}
				is CameraUIAction.OnCameraClickLuz -> {
				}
			}
			return@CameraPreviewView true
		}
	)
}


@Composable
private fun CameraPreviewView(
	uriArqvBranco: Uri?,
	uriArqvLamb1: Uri?,
	uriArqvLamb2: Uri?,
	uriArqvSpectre: Uri?,
	uriCamBranco: Uri?,
	uriCamLamb1: Uri?,
	uriCamLamb2: Uri?,
	uriCamSpectre: Uri?,
	imageCapture: ImageCapture,
	lensFacing: Int = CameraSelector.LENS_FACING_BACK,
	cameraUIAction: (CameraUIAction) -> Boolean,
) {

	var uriBrancoAnal: Uri? = null
	var uriLamb1Anal: Uri? = null
	var uriLamb2Anal: Uri? = null
	var uriSpectreAnal: Uri? = null

	val context = LocalContext.current
	val lifecycleOwner = LocalLifecycleOwner.current

	val preview = Preview.Builder().build()
	val cameraSelector = CameraSelector.Builder()
		.requireLensFacing(lensFacing)
		.build()

	val viewPort =  ViewPort.Builder(Rational(100, 100), ROTATION_0).build()

	val previewView = remember { PreviewView(context) }
	var torchOn by remember { mutableStateOf(false) }

	var camera by remember { mutableStateOf<Camera?>(null) }
	LaunchedEffect(lensFacing) {
		val cameraProvider = context.getCameraProvider()
		val useCaseGroup = UseCaseGroup.Builder()
			.addUseCase(preview)
			.addUseCase(imageCapture)
			.setViewPort(viewPort)
			.build()
		cameraProvider.unbindAll()
		camera = cameraProvider.bindToLifecycle(
			lifecycleOwner,
			cameraSelector,
			useCaseGroup
		)
		preview.setSurfaceProvider(previewView.surfaceProvider)
	}


	var corretor = listOf<Float>(0f, 3f)

	var calculando = false

	var inputSensibilidade by remember {
		mutableStateOf<String>("80")
	}
	var inputEspessura by remember {
		mutableStateOf<String>("10")
	}
	var inputPosicao by remember {
		mutableStateOf<String>("10")
	}

	val lamb1Inicial = "532"
	val lamb2Inicial = "650"
	var inputLamb1 by remember {
		mutableStateOf<String>(lamb1Inicial)
	}
	var inputLamb2 by remember {
		mutableStateOf<String>(lamb2Inicial)
	}

	var orientacaoSpec by remember {
		mutableStateOf(5)
	}

	val posicaoInicial = "50"
	var posicao by remember {
		mutableStateOf<String>(posicaoInicial)
	}

	var avisos by remember {
		mutableStateOf<String>("")
	}

	var medidas: List<List<Float>>


	var points = listOf(.40f, .0f, .20f, .20f, .50f, .60f, .80f, .40f, .1f, .0f)
	
	var inicio by remember {
		mutableStateOf(531)
	}
	var final: Int? by remember {
		mutableStateOf(null)
	}
	var abcisas:List<Float>? = null
	val defaultInicio: Int? = null

	val vetLambThor = listOf(
	//	398.10f, 398.22f, 398.33f, 398.44f, 398.55f, 398.66f, 398.77f, 398.88f, 398.99f, 399.11f, 399.22f, 399.33f, 399.44f, 399.55f, 399.66f, 399.77f, 399.89f, 400.00f, 400.11f, 400.22f, 400.33f, 400.44f, 400.55f, 400.67f, 400.78f, 400.89f, 401.00f, 401.11f, 401.22f, 401.33f, 401.45f, 401.56f, 401.67f, 401.78f, 401.89f, 402.00f, 402.11f, 402.23f, 402.34f, 402.45f, 402.56f, 402.67f, 402.78f, 402.89f, 403.01f, 403.12f, 403.23f, 403.34f, 403.45f, 403.56f, 403.68f, 403.79f, 403.90f, 404.01f, 404.12f, 404.23f, 404.34f, 404.46f, 404.57f, 404.68f, 404.79f, 404.90f, 405.01f, 405.13f, 405.24f, 405.35f, 405.46f, 405.57f, 405.68f, 405.80f, 405.91f, 406.02f, 406.13f, 406.24f, 406.35f, 406.47f, 406.58f, 406.69f, 406.80f, 406.91f, 407.02f, 407.14f, 407.25f, 407.36f, 407.47f, 407.58f, 407.70f, 407.81f, 407.92f, 408.03f, 408.14f, 408.25f, 408.37f, 408.48f, 408.59f, 408.70f, 408.81f, 408.93f, 409.04f, 409.15f, 409.26f, 409.37f, 409.48f, 409.60f, 409.71f, 409.82f, 409.93f, 410.04f, 410.16f, 410.27f, 410.38f, 410.49f, 410.60f, 410.72f, 410.83f, 410.94f, 411.05f, 411.16f, 411.28f, 411.39f, 411.50f, 411.61f, 411.72f, 411.83f, 411.95f, 412.06f, 412.17f, 412.28f, 412.39f, 412.51f, 412.62f, 412.73f, 412.84f, 412.95f, 413.07f, 413.18f, 413.29f, 413.40f, 413.52f, 413.63f, 413.74f, 413.85f, 413.96f, 414.08f, 414.19f, 414.30f, 414.41f, 414.52f, 414.64f, 414.75f, 414.86f, 414.97f, 415.08f, 415.20f, 415.31f, 415.42f, 415.53f, 415.65f, 415.76f, 415.87f, 415.98f, 416.09f, 416.21f, 416.32f, 416.43f, 416.54f, 416.65f, 416.77f, 416.88f, 416.99f, 417.10f, 417.22f, 417.33f, 417.44f, 417.55f, 417.67f, 417.78f, 417.89f, 418.00f, 418.11f, 418.23f, 418.34f, 418.45f, 418.56f, 418.68f, 418.79f, 418.90f, 419.01f, 419.13f, 419.24f, 419.35f, 419.46f, 419.57f, 419.69f, 419.80f, 419.91f, 420.02f, 420.14f, 420.25f, 420.36f, 420.47f, 420.59f, 420.70f, 420.81f, 420.92f, 421.04f, 421.15f, 421.26f, 421.37f, 421.49f, 421.60f, 421.71f, 421.82f, 421.94f, 422.05f, 422.16f, 422.27f, 422.39f, 422.50f, 422.61f, 422.72f, 422.83f, 422.95f, 423.06f, 423.17f, 423.29f, 423.40f, 423.51f, 423.62f, 423.74f, 423.85f, 423.96f, 424.07f, 424.19f, 424.30f, 424.41f, 424.52f, 424.64f, 424.75f, 424.86f, 424.97f, 425.09f, 425.20f, 425.31f, 425.42f, 425.54f, 425.65f, 425.76f, 425.87f, 425.99f, 426.10f, 426.21f, 426.33f, 426.44f, 426.55f, 426.66f, 426.78f, 426.89f, 427.00f, 427.11f, 427.23f, 427.34f, 427.45f, 427.57f, 427.68f, 427.79f, 427.90f, 428.02f, 428.13f, 428.24f, 428.35f, 428.47f, 428.58f, 428.69f, 428.81f, 428.92f, 429.03f, 429.14f, 429.26f, 429.37f, 429.48f, 429.60f, 429.71f, 429.82f, 429.93f, 430.05f, 430.16f, 430.27f, 430.39f, 430.50f, 430.61f, 430.72f, 430.84f, 430.95f, 431.06f, 431.18f, 431.29f, 431.40f, 431.51f, 431.63f, 431.74f, 431.85f, 431.97f, 432.08f, 432.19f, 432.30f, 432.42f, 432.53f, 432.64f, 432.76f, 432.87f, 432.98f, 433.10f, 433.21f, 433.32f, 433.43f, 433.55f, 433.66f, 433.77f, 433.89f, 434.00f, 434.11f, 434.23f, 434.34f, 434.45f, 434.56f, 434.68f, 434.79f, 434.90f, 435.02f, 435.13f, 435.24f, 435.36f, 435.47f, 435.58f, 435.70f, 435.81f, 435.92f, 436.03f, 436.15f, 436.26f, 436.37f, 436.49f, 436.60f, 436.71f, 436.83f, 436.94f, 437.05f, 437.17f, 437.28f, 437.39f, 437.51f, 437.62f, 437.73f, 437.85f, 437.96f, 438.07f, 438.19f, 438.30f, 438.41f, 438.52f, 438.64f, 438.75f, 438.86f, 438.98f, 439.09f, 439.20f, 439.32f, 439.43f, 439.54f, 439.66f, 439.77f, 439.88f, 440.00f, 440.11f, 440.22f, 440.34f, 440.45f, 440.56f, 440.68f, 440.79f, 440.90f, 441.02f, 441.13f, 441.24f, 441.36f, 441.47f, 441.58f, 441.70f, 441.81f, 441.92f, 442.04f, 442.15f, 442.26f, 442.38f, 442.49f, 442.60f, 442.72f, 442.83f, 442.94f, 443.06f, 443.17f, 443.29f, 443.40f, 443.51f, 443.63f, 443.74f, 443.85f, 443.97f, 444.08f, 444.19f, 444.31f, 444.42f, 444.53f, 444.65f, 444.76f, 444.87f, 444.99f, 445.10f, 445.21f, 445.33f, 445.44f, 445.55f, 445.67f, 445.78f, 445.90f, 446.01f, 446.12f, 446.24f, 446.35f, 446.46f, 446.58f, 446.69f, 446.80f, 446.92f, 447.03f, 447.14f, 447.26f, 447.37f, 447.49f, 447.60f, 447.71f, 447.83f, 447.94f, 448.05f, 448.17f, 448.28f, 448.39f, 448.51f, 448.62f, 448.74f, 448.85f, 448.96f, 449.08f, 449.19f, 449.30f, 449.42f, 449.53f, 449.65f, 449.76f, 449.87f, 449.99f, 450.10f, 450.21f, 450.33f, 450.44f, 450.56f, 450.67f, 450.78f, 450.90f, 451.01f, 451.12f, 451.24f, 451.35f, 451.47f, 451.58f, 451.69f, 451.81f, 451.92f, 452.03f, 452.15f, 452.26f, 452.38f, 452.49f, 452.60f, 452.72f, 452.83f, 452.95f, 453.06f, 453.17f, 453.29f, 453.40f, 453.51f, 453.63f, 453.74f, 453.86f, 453.97f, 454.08f, 454.20f, 454.31f, 454.43f, 454.54f, 454.65f, 454.77f, 454.88f, 455.00f, 455.11f, 455.22f, 455.34f, 455.45f, 455.57f, 455.68f, 455.79f, 455.91f, 456.02f, 456.13f, 456.25f, 456.36f, 456.48f, 456.59f, 456.70f, 456.82f, 456.93f, 457.05f, 457.16f, 457.28f, 457.39f, 457.50f, 457.62f, 457.73f, 457.85f, 457.96f, 458.07f, 458.19f, 458.30f, 458.42f, 458.53f, 458.64f, 458.76f, 458.87f, 458.99f, 459.10f, 459.21f, 459.33f, 459.44f, 459.56f, 459.67f, 459.78f, 459.90f, 460.01f, 460.13f, 460.24f, 460.36f, 460.47f, 460.58f, 460.70f, 460.81f, 460.93f, 461.04f, 461.15f, 461.27f, 461.38f, 461.50f, 461.61f, 461.73f, 461.84f, 461.95f, 462.07f, 462.18f, 462.30f, 462.41f, 462.53f, 462.64f, 462.75f, 462.87f, 462.98f, 463.10f, 463.21f, 463.33f, 463.44f, 463.55f, 463.67f, 463.78f, 463.90f, 464.01f, 464.13f, 464.24f, 464.35f, 464.47f, 464.58f, 464.70f, 464.81f, 464.93f, 465.04f, 465.15f, 465.27f, 465.38f, 465.50f, 465.61f, 465.73f, 465.84f, 465.95f, 466.07f, 466.18f, 466.30f, 466.41f, 466.53f, 466.64f, 466.76f, 466.87f, 466.98f, 467.10f, 467.21f, 467.33f, 467.44f, 467.56f, 467.67f, 467.79f, 467.90f, 468.01f, 468.13f, 468.24f, 468.36f, 468.47f, 468.59f, 468.70f, 468.82f, 468.93f, 469.04f, 469.16f, 469.27f, 469.39f, 469.50f, 469.62f, 469.73f, 469.85f, 469.96f, 470.08f, 470.19f, 470.30f, 470.42f, 470.53f, 470.65f, 470.76f, 470.88f, 470.99f, 471.11f, 471.22f, 471.34f, 471.45f, 471.56f, 471.68f, 471.79f, 471.91f, 472.02f, 472.14f, 472.25f, 472.37f, 472.48f, 472.60f, 472.71f, 472.83f, 472.94f, 473.06f, 473.17f, 473.28f, 473.40f, 473.51f, 473.63f, 473.74f, 473.86f, 473.97f, 474.09f, 474.20f, 474.32f, 474.43f, 474.55f, 474.66f, 474.78f, 474.89f, 475.00f, 475.12f, 475.23f, 475.35f, 475.46f, 475.58f, 475.69f, 475.81f, 475.92f, 476.04f, 476.15f, 476.27f, 476.38f, 476.50f, 476.61f, 476.73f, 476.84f, 476.96f, 477.07f, 477.19f, 477.30f, 477.42f, 477.53f, 477.64f, 477.76f, 477.87f, 477.99f, 478.10f, 478.22f, 478.33f, 478.45f, 478.56f, 478.68f, 478.79f, 478.91f, 479.02f, 479.14f, 479.25f, 479.37f, 479.48f, 479.60f, 479.71f, 479.83f, 479.94f, 480.06f, 480.17f, 480.29f, 480.40f, 480.52f, 480.63f, 480.75f, 480.86f, 480.98f, 481.09f, 481.21f, 481.32f, 481.44f, 481.55f, 481.67f, 481.78f, 481.90f, 482.01f, 482.13f, 482.24f, 482.36f, 482.47f, 482.59f, 482.70f, 482.82f, 482.93f, 483.05f, 483.16f, 483.28f, 483.39f, 483.51f, 483.62f, 483.74f, 483.85f, 483.97f, 484.08f, 484.20f, 484.31f, 484.43f, 484.54f, 484.66f, 484.77f, 484.89f, 485.00f, 485.12f, 485.23f, 485.35f, 485.46f, 485.58f, 485.69f, 485.81f, 485.92f, 486.04f, 486.15f, 486.27f, 486.38f, 486.50f, 486.61f, 486.73f, 486.84f, 486.96f, 487.07f, 487.19f, 487.30f, 487.42f, 487.54f, 487.65f, 487.77f, 487.88f, 488.00f, 488.11f, 488.23f, 488.34f, 488.46f, 488.57f, 488.69f, 488.80f, 488.92f, 489.03f, 489.15f, 489.26f, 489.38f, 489.49f, 489.61f, 489.72f, 489.84f, 489.96f, 490.07f, 490.19f, 490.30f, 490.42f, 490.53f, 490.65f, 490.76f, 490.88f, 490.99f, 491.11f, 491.22f, 491.34f, 491.45f, 491.57f, 491.68f, 491.80f, 491.92f, 492.03f, 492.15f, 492.26f, 492.38f, 492.49f, 492.61f, 492.72f, 492.84f, 492.95f, 493.07f, 493.18f, 493.30f, 493.41f, 493.53f, 493.65f, 493.76f, 493.88f, 493.99f, 494.11f, 494.22f, 494.34f, 494.45f, 494.57f, 494.68f, 494.80f, 494.92f, 495.03f, 495.15f, 495.26f, 495.38f, 495.49f, 495.61f, 495.72f, 495.84f, 495.95f, 496.07f, 496.19f, 496.30f, 496.42f, 496.53f, 496.65f, 496.76f, 496.88f, 496.99f, 497.11f, 497.22f, 497.34f, 497.46f, 497.57f, 497.69f, 497.80f, 497.92f, 498.03f, 498.15f, 498.26f, 498.38f, 498.50f, 498.61f, 498.73f, 498.84f, 498.96f, 499.07f, 499.19f, 499.30f, 499.42f, 499.54f, 499.65f, 499.77f, 499.88f, 500.00f, 500.11f, 500.23f, 500.35f, 500.46f, 500.58f, 500.69f, 500.81f, 500.92f, 501.04f, 501.15f, 501.27f, 501.39f, 501.50f, 501.62f, 501.73f, 501.85f, 501.96f, 502.08f, 502.20f, 502.31f, 502.43f, 502.54f, 502.66f, 502.77f, 502.89f, 503.01f, 503.12f, 503.24f, 503.35f, 503.47f, 503.58f, 503.70f, 503.82f, 503.93f, 504.05f, 504.16f, 504.28f, 504.39f, 504.51f, 504.63f, 504.74f, 504.86f, 504.97f, 505.09f, 505.20f, 505.32f, 505.44f, 505.55f, 505.67f, 505.78f, 505.90f, 506.02f, 506.13f, 506.25f, 506.36f, 506.48f, 506.59f, 506.71f, 506.83f, 506.94f, 507.06f, 507.17f, 507.29f, 507.41f, 507.52f, 507.64f, 507.75f, 507.87f, 507.98f, 508.10f, 508.22f, 508.33f, 508.45f, 508.56f, 508.68f, 508.80f, 508.91f, 509.03f, 509.14f, 509.26f, 509.38f, 509.49f, 509.61f, 509.72f, 509.84f, 509.95f, 510.07f, 510.19f, 510.30f, 510.42f, 510.53f, 510.65f, 510.77f, 510.88f, 511.00f, 511.11f, 511.23f, 511.35f, 511.46f, 511.58f, 511.69f, 511.81f, 511.93f, 512.04f, 512.16f, 512.27f, 512.39f, 512.51f, 512.62f, 512.74f, 512.85f, 512.97f, 513.09f, 513.20f, 513.32f, 513.43f, 513.55f, 513.67f, 513.78f, 513.90f, 514.01f, 514.13f, 514.25f, 514.36f, 514.48f, 514.60f, 514.71f, 514.83f, 514.94f, 515.06f, 515.18f, 515.29f, 515.41f, 515.52f, 515.64f, 515.76f, 515.87f, 515.99f, 516.10f, 516.22f, 516.34f, 516.45f, 516.57f, 516.69f, 516.80f, 516.92f, 517.03f, 517.15f, 517.27f, 517.38f, 517.50f, 517.61f, 517.73f, 517.85f, 517.96f, 518.08f, 518.20f, 518.31f, 518.43f, 518.54f, 518.66f, 518.78f, 518.89f, 519.01f, 519.12f, 519.24f, 519.36f, 519.47f, 519.59f, 519.71f, 519.82f, 519.94f, 520.05f, 520.17f, 520.29f, 520.40f, 520.52f, 520.64f, 520.75f, 520.87f, 520.98f, 521.10f, 521.22f, 521.33f, 521.45f, 521.57f, 521.68f, 521.80f, 521.91f, 522.03f, 522.15f, 522.26f, 522.38f, 522.50f, 522.61f, 522.73f, 522.85f, 522.96f, 523.08f, 523.19f, 523.31f, 523.43f, 523.54f, 523.66f, 523.78f, 523.89f, 524.01f, 524.12f, 524.24f, 524.36f, 524.47f, 524.59f, 524.71f, 524.82f, 524.94f, 525.06f, 525.17f, 525.29f, 525.41f, 525.52f, 525.64f, 525.75f, 525.87f, 525.99f, 526.10f, 526.22f, 526.34f, 526.45f, 526.57f, 526.69f, 526.80f, 526.92f, 527.04f, 527.15f, 527.27f, 527.38f, 527.50f, 527.62f, 527.73f, 527.85f, 527.97f, 528.08f, 528.20f, 528.32f, 528.43f, 528.55f, 528.67f, 528.78f, 528.90f, 529.02f, 529.13f, 529.25f, 529.36f, 529.48f, 529.60f, 529.71f, 529.83f, 529.95f, 530.06f, 530.18f, 530.30f, 530.41f, 530.53f, 530.65f, 530.76f, 530.88f, 531.00f, 531.11f, 531.23f, 531.35f, 531.46f, 531.58f, 531.70f, 531.81f, 531.93f, 532.05f, 532.16f, 532.28f, 532.39f, 532.51f, 532.63f, 532.74f, 532.86f, 532.98f, 533.09f, 533.21f, 533.33f, 533.44f, 533.56f, 533.68f, 533.79f, 533.91f, 534.03f, 534.14f, 534.26f, 534.38f, 534.49f, 534.61f, 534.73f, 534.84f, 534.96f, 535.08f, 535.19f, 535.31f, 535.43f, 535.54f, 535.66f, 535.78f, 535.89f, 536.01f, 536.13f, 536.24f, 536.36f, 536.48f, 536.59f, 536.71f, 536.83f, 536.94f, 537.06f, 537.18f, 537.29f, 537.41f, 537.53f, 537.64f, 537.76f, 537.88f, 537.99f, 538.11f, 538.23f, 538.35f, 538.46f, 538.58f, 538.70f, 538.81f, 538.93f, 539.05f, 539.16f, 539.28f, 539.40f, 539.51f, 539.63f, 539.75f, 539.86f, 539.98f, 540.10f, 540.21f, 540.33f, 540.45f, 540.56f, 540.68f, 540.80f, 540.91f, 541.03f, 541.15f, 541.26f, 541.38f, 541.50f, 541.62f, 541.73f, 541.85f, 541.97f, 542.08f, 542.20f, 542.32f, 542.43f, 542.55f, 542.67f, 542.78f, 542.90f, 543.02f, 543.13f, 543.25f, 543.37f, 543.48f, 543.60f, 543.72f, 543.84f, 543.95f, 544.07f, 544.19f, 544.30f, 544.42f, 544.54f, 544.65f, 544.77f, 544.89f, 545.00f, 545.12f, 545.24f, 545.36f, 545.47f, 545.59f, 545.71f, 545.82f, 545.94f, 546.06f, 546.17f, 546.29f, 546.41f, 546.52f, 546.64f, 546.76f, 546.88f, 546.99f, 547.11f, 547.23f, 547.34f, 547.46f, 547.58f, 547.69f, 547.81f, 547.93f, 548.05f, 548.16f, 548.28f, 548.40f, 548.51f, 548.63f, 548.75f, 548.86f, 548.98f, 549.10f, 549.22f, 549.33f, 549.45f, 549.57f, 549.68f, 549.80f, 549.92f, 550.04f, 550.15f, 550.27f, 550.39f, 550.50f, 550.62f, 550.74f, 550.85f, 550.97f, 551.09f, 551.21f, 551.32f, 551.44f, 551.56f, 551.67f, 551.79f, 551.91f, 552.03f, 552.14f, 552.26f, 552.38f, 552.49f, 552.61f, 552.73f, 552.85f, 552.96f, 553.08f, 553.20f, 553.31f, 553.43f, 553.55f, 553.66f, 553.78f, 553.90f, 554.02f, 554.13f, 554.25f, 554.37f, 554.48f, 554.60f, 554.72f, 554.84f, 554.95f, 555.07f, 555.19f, 555.31f, 555.42f, 555.54f, 555.66f, 555.77f, 555.89f, 556.01f, 556.13f, 556.24f, 556.36f, 556.48f, 556.59f, 556.71f, 556.83f, 556.95f, 557.06f, 557.18f, 557.30f, 557.41f, 557.53f, 557.65f, 557.77f, 557.88f, 558.00f, 558.12f, 558.24f, 558.35f, 558.47f, 558.59f, 558.70f, 558.82f, 558.94f, 559.06f, 559.17f, 559.29f, 559.41f, 559.53f, 559.64f, 559.76f, 559.88f, 559.99f, 560.11f, 560.23f, 560.35f, 560.46f, 560.58f, 560.70f, 560.82f, 560.93f, 561.05f, 561.17f, 561.28f, 561.40f, 561.52f, 561.64f, 561.75f, 561.87f, 561.99f, 562.11f, 562.22f, 562.34f, 562.46f, 562.58f, 562.69f, 562.81f, 562.93f, 563.04f, 563.16f, 563.28f, 563.40f, 563.51f, 563.63f, 563.75f, 563.87f, 563.98f, 564.10f, 564.22f, 564.34f, 564.45f, 564.57f, 564.69f, 564.81f, 564.92f, 565.04f, 565.16f, 565.27f, 565.39f, 565.51f, 565.63f, 565.74f, 565.86f, 565.98f, 566.10f, 566.21f, 566.33f, 566.45f, 566.57f, 566.68f, 566.80f, 566.92f, 567.04f, 567.15f, 567.27f, 567.39f, 567.51f, 567.62f, 567.74f, 567.86f, 567.98f, 568.09f, 568.21f, 568.33f, 568.45f, 568.56f, 568.68f, 568.80f, 568.92f, 569.03f, 569.15f, 569.27f, 569.39f, 569.50f, 569.62f, 569.74f, 569.86f, 569.97f, 570.09f, 570.21f, 570.33f, 570.44f, 570.56f, 570.68f, 570.80f, 570.91f, 571.03f, 571.15f, 571.27f, 571.38f, 571.50f, 571.62f, 571.74f, 571.85f, 571.97f, 572.09f, 572.21f, 572.32f, 572.44f, 572.56f, 572.68f, 572.79f, 572.91f, 573.03f, 573.15f, 573.26f, 573.38f, 573.50f, 573.62f, 573.73f, 573.85f, 573.97f, 574.09f, 574.20f, 574.32f, 574.44f, 574.56f, 574.67f, 574.79f, 574.91f, 575.03f, 575.15f, 575.26f, 575.38f, 575.50f, 575.62f, 575.73f, 575.85f, 575.97f, 576.09f, 576.20f, 576.32f, 576.44f, 576.56f, 576.67f, 576.79f, 576.91f, 577.03f, 577.14f, 577.26f, 577.38f, 577.50f, 577.62f, 577.73f, 577.85f, 577.97f, 578.09f, 578.20f, 578.32f, 578.44f, 578.56f, 578.67f, 578.79f, 578.91f, 579.03f, 579.15f, 579.26f, 579.38f, 579.50f, 579.62f, 579.73f, 579.85f, 579.97f, 580.09f, 580.20f, 580.32f, 580.44f, 580.56f, 580.68f, 580.79f, 580.91f, 581.03f, 581.15f, 581.26f, 581.38f, 581.50f, 581.62f, 581.73f, 581.85f, 581.97f, 582.09f, 582.21f, 582.32f, 582.44f, 582.56f, 582.68f, 582.79f, 582.91f, 583.03f, 583.15f, 583.27f, 583.38f, 583.50f, 583.62f, 583.74f, 583.85f, 583.97f, 584.09f, 584.21f, 584.33f, 584.44f, 584.56f, 584.68f, 584.80f, 584.91f, 585.03f, 585.15f, 585.27f, 585.39f, 585.50f, 585.62f, 585.74f, 585.86f, 585.97f, 586.09f, 586.21f, 586.33f, 586.45f, 586.56f, 586.68f, 586.80f, 586.92f, 587.04f, 587.15f, 587.27f, 587.39f, 587.51f, 587.62f, 587.74f, 587.86f, 587.98f, 588.10f, 588.21f, 588.33f, 588.45f, 588.57f, 588.69f, 588.80f, 588.92f, 589.04f, 589.16f, 589.27f, 589.39f, 589.51f, 589.63f, 589.75f, 589.86f, 589.98f, 590.10f, 590.22f, 590.34f, 590.45f, 590.57f, 590.69f, 590.81f, 590.92f, 591.04f, 591.16f, 591.28f, 591.40f, 591.51f, 591.63f, 591.75f, 591.87f, 591.99f, 592.10f, 592.22f, 592.34f, 592.46f, 592.58f, 592.69f, 592.81f, 592.93f, 593.05f, 593.17f, 593.28f, 593.40f, 593.52f, 593.64f, 593.76f, 593.87f, 593.99f, 594.11f, 594.23f, 594.35f, 594.46f, 594.58f, 594.70f, 594.82f, 594.93f, 595.05f, 595.17f, 595.29f, 595.41f, 595.52f, 595.64f, 595.76f, 595.88f, 596.00f, 596.11f, 596.23f, 596.35f, 596.47f, 596.59f, 596.70f, 596.82f, 596.94f, 597.06f, 597.18f, 597.29f, 597.41f, 597.53f, 597.65f, 597.77f, 597.89f, 598.00f, 598.12f, 598.24f, 598.36f, 598.48f, 598.59f, 598.71f, 598.83f, 598.95f, 599.07f, 599.18f, 599.30f, 599.42f, 599.54f, 599.66f, 599.77f, 599.89f, 600.01f, 600.13f, 600.25f, 600.36f, 600.48f, 600.60f, 600.72f, 600.84f, 600.95f, 601.07f, 601.19f, 601.31f, 601.43f, 601.54f, 601.66f, 601.78f, 601.90f, 602.02f, 602.14f, 602.25f, 602.37f, 602.49f, 602.61f, 602.73f, 602.84f, 602.96f, 603.08f, 603.20f, 603.32f, 603.43f, 603.55f, 603.67f, 603.79f, 603.91f, 604.02f, 604.14f, 604.26f, 604.38f, 604.50f, 604.62f, 604.73f, 604.85f, 604.97f, 605.09f, 605.21f, 605.32f, 605.44f, 605.56f, 605.68f, 605.80f, 605.92f, 606.03f, 606.15f, 606.27f, 606.39f, 606.51f, 606.62f, 606.74f, 606.86f, 606.98f, 607.10f, 607.21f, 607.33f, 607.45f, 607.57f, 607.69f, 607.81f, 607.92f, 608.04f, 608.16f, 608.28f, 608.40f, 608.51f, 608.63f, 608.75f, 608.87f, 608.99f, 609.11f, 609.22f, 609.34f, 609.46f, 609.58f, 609.70f, 609.82f, 609.93f, 610.05f, 610.17f, 610.29f, 610.41f, 610.52f, 610.64f, 610.76f, 610.88f, 611.00f, 611.12f, 611.23f, 611.35f, 611.47f, 611.59f, 611.71f, 611.82f, 611.94f, 612.06f, 612.18f, 612.30f, 612.42f, 612.53f, 612.65f, 612.77f, 612.89f, 613.01f, 613.13f, 613.24f, 613.36f, 613.48f, 613.60f, 613.72f, 613.84f, 613.95f, 614.07f, 614.19f, 614.31f, 614.43f, 614.54f, 614.66f, 614.78f, 614.90f, 615.02f, 615.14f, 615.25f, 615.37f, 615.49f, 615.61f, 615.73f, 615.85f, 615.96f, 616.08f, 616.20f, 616.32f, 616.44f, 616.56f, 616.67f, 616.79f, 616.91f, 617.03f, 617.15f, 617.27f, 617.38f, 617.50f, 617.62f, 617.74f, 617.86f, 617.98f, 618.09f, 618.21f, 618.33f, 618.45f, 618.57f, 618.69f, 618.80f, 618.92f, 619.04f, 619.16f, 619.28f, 619.40f, 619.51f, 619.63f, 619.75f, 619.87f, 619.99f, 620.11f, 620.22f, 620.34f, 620.46f, 620.58f, 620.70f, 620.82f, 620.93f, 621.05f, 621.17f, 621.29f, 621.41f, 621.53f, 621.64f, 621.76f, 621.88f, 622.00f, 622.12f, 622.24f, 622.35f, 622.47f, 622.59f, 622.71f, 622.83f, 622.95f, 623.06f, 623.18f, 623.30f, 623.42f, 623.54f, 623.66f, 623.78f, 623.89f, 624.01f, 624.13f, 624.25f, 624.37f, 624.49f, 624.60f, 624.72f, 624.84f, 624.96f, 625.08f, 625.20f, 625.31f, 625.43f, 625.55f, 625.67f, 625.79f, 625.91f, 626.02f, 626.14f, 626.26f, 626.38f, 626.50f, 626.62f, 626.74f, 626.85f, 626.97f, 627.09f, 627.21f, 627.33f, 627.45f, 627.56f, 627.68f, 627.80f, 627.92f, 628.04f, 628.16f, 628.28f, 628.39f, 628.51f, 628.63f, 628.75f, 628.87f, 628.99f, 629.10f, 629.22f, 629.34f, 629.46f, 629.58f, 629.70f, 629.82f, 629.93f, 630.05f, 630.17f, 630.29f, 630.41f, 630.53f, 630.64f, 630.76f, 630.88f, 631.00f, 631.12f, 631.24f, 631.36f, 631.47f, 631.59f, 631.71f, 631.83f, 631.95f, 632.07f, 632.18f, 632.30f, 632.42f, 632.54f, 632.66f, 632.78f, 632.90f, 633.01f, 633.13f, 633.25f, 633.37f, 633.49f, 633.61f, 633.73f, 633.84f, 633.96f, 634.08f, 634.20f, 634.32f, 634.44f, 634.55f, 634.67f, 634.79f, 634.91f, 635.03f, 635.15f, 635.27f, 635.38f, 635.50f, 635.62f, 635.74f, 635.86f, 635.98f, 636.10f, 636.21f, 636.33f, 636.45f, 636.57f, 636.69f, 636.81f, 636.93f, 637.04f, 637.16f, 637.28f, 637.40f, 637.52f, 637.64f, 637.76f, 637.87f, 637.99f, 638.11f, 638.23f, 638.35f, 638.47f, 638.59f, 638.70f, 638.82f, 638.94f, 639.06f, 639.18f, 639.30f, 639.42f, 639.53f, 639.65f, 639.77f, 639.89f, 640.01f, 640.13f, 640.25f, 640.36f, 640.48f, 640.60f, 640.72f, 640.84f, 640.96f, 641.08f, 641.19f, 641.31f, 641.43f, 641.55f, 641.67f, 641.79f, 641.91f, 642.02f, 642.14f, 642.26f, 642.38f, 642.50f, 642.62f, 642.74f, 642.85f, 642.97f, 643.09f, 643.21f, 643.33f, 643.45f, 643.57f, 643.68f, 643.80f, 643.92f, 644.04f, 644.16f, 644.28f, 644.40f, 644.51f, 644.63f, 644.75f, 644.87f, 644.99f, 645.11f, 645.23f, 645.34f, 645.46f, 645.58f, 645.70f, 645.82f, 645.94f, 646.06f, 646.18f, 646.29f, 646.41f, 646.53f, 646.65f, 646.77f, 646.89f, 647.01f, 647.12f, 647.24f, 647.36f, 647.48f, 647.60f, 647.72f, 647.84f, 647.95f, 648.07f, 648.19f, 648.31f, 648.43f, 648.55f, 648.67f, 648.79f, 648.90f, 649.02f, 649.14f, 649.26f, 649.38f, 649.50f, 649.62f, 649.73f, 649.85f, 649.97f, 650.09f, 650.21f, 650.33f, 650.45f, 650.57f, 650.68f, 650.80f, 650.92f, 651.04f, 651.16f, 651.28f, 651.40f, 651.51f, 651.63f, 651.75f, 651.87f, 651.99f, 652.11f, 652.23f, 652.35f, 652.46f, 652.58f, 652.70f, 652.82f, 652.94f, 653.06f, 653.18f, 653.29f, 653.41f, 653.53f, 653.65f, 653.77f, 653.89f, 654.01f, 654.13f, 654.24f, 654.36f, 654.48f, 654.60f, 654.72f, 654.84f, 654.96f, 655.08f, 655.19f, 655.31f, 655.43f, 655.55f, 655.67f, 655.79f, 655.91f, 656.02f, 656.14f, 656.26f, 656.38f, 656.50f, 656.62f, 656.74f, 656.86f, 656.97f, 657.09f, 657.21f, 657.33f, 657.45f, 657.57f, 657.69f, 657.81f, 657.92f, 658.04f, 658.16f, 658.28f, 658.40f, 658.52f, 658.64f, 658.76f, 658.87f, 658.99f, 659.11f, 659.23f, 659.35f, 659.47f, 659.59f, 659.71f, 659.82f, 659.94f, 660.06f, 660.18f, 660.30f, 660.42f, 660.54f, 660.66f, 660.77f, 660.89f, 661.01f, 661.13f, 661.25f, 661.37f, 661.49f, 661.60f, 661.72f, 661.84f, 661.96f, 662.08f, 662.20f, 662.32f, 662.44f, 662.55f, 662.67f, 662.79f, 662.91f, 663.03f, 663.15f, 663.27f, 663.39f, 663.50f, 663.62f, 663.74f, 663.86f, 663.98f, 664.10f, 664.22f, 664.34f, 664.46f, 664.57f, 664.69f, 664.81f, 664.93f, 665.05f, 665.17f, 665.29f, 665.41f, 665.52f, 665.64f, 665.76f, 665.88f, 666.00f, 666.12f, 666.24f, 666.36f, 666.47f, 666.59f, 666.71f, 666.83f, 666.95f, 667.07f, 667.19f, 667.31f, 667.42f, 667.54f, 667.66f, 667.78f, 667.90f, 668.02f, 668.14f, 668.26f, 668.37f, 668.49f, 668.61f, 668.73f, 668.85f, 668.97f, 669.09f, 669.21f, 669.32f, 669.44f, 669.56f, 669.68f, 669.80f, 669.92f, 670.04f, 670.16f, 670.28f, 670.39f, 670.51f, 670.63f, 670.75f, 670.87f, 670.99f, 671.11f, 671.23f, 671.34f, 671.46f, 671.58f, 671.70f, 671.82f, 671.94f, 672.06f, 672.18f, 672.29f, 672.41f, 672.53f, 672.65f, 672.77f, 672.89f, 673.01f, 673.13f, 673.24f, 673.36f, 673.48f, 673.60f, 673.72f, 673.84f, 673.96f, 674.08f, 674.20f, 674.31f, 674.43f, 674.55f, 674.67f, 674.79f, 674.91f, 675.03f, 675.15f, 675.26f, 675.38f, 675.50f, 675.62f, 675.74f, 675.86f, 675.98f, 676.10f, 676.22f, 676.33f, 676.45f, 676.57f, 676.69f, 676.81f, 676.93f, 677.05f, 677.17f, 677.28f, 677.40f, 677.52f, 677.64f, 677.76f, 677.88f, 678.00f, 678.12f, 678.24f, 678.35f, 678.47f, 678.59f, 678.71f, 678.83f, 678.95f, 679.07f, 679.19f, 679.30f, 679.42f, 679.54f, 679.66f, 679.78f, 679.90f, 680.02f, 680.14f, 680.26f, 680.37f, 680.49f, 680.61f, 680.73f, 680.85f, 680.97f, 681.09f, 681.21f, 681.32f, 681.44f, 681.56f, 681.68f, 681.80f, 681.92f, 682.04f, 682.16f, 682.28f, 682.39f, 682.51f, 682.63f, 682.75f, 682.87f, 682.99f, 683.11f, 683.23f, 683.35f, 683.46f, 683.58f, 683.70f, 683.82f, 683.94f, 684.06f, 684.18f, 684.30f, 684.41f, 684.53f, 684.65f, 684.77f, 684.89f, 685.01f, 685.13f, 685.25f, 685.37f, 685.48f, 685.60f, 685.72f, 685.84f, 685.96f, 686.08f, 686.20f, 686.32f, 686.43f, 686.55f, 686.67f, 686.79f, 686.91f, 687.03f, 687.15f, 687.27f, 687.39f, 687.50f, 687.62f, 687.74f, 687.86f, 687.98f, 688.10f, 688.22f, 688.34f, 688.46f, 688.57f, 688.69f, 688.81f, 688.93f, 689.05f, 689.17f, 689.29f, 689.41f, 689.53f, 689.64f, 689.76f, 689.88f, 690.00f, 690.12f, 690.24f, 690.36f, 690.48f, 690.59f, 690.71f, 690.83f, 690.95f, 691.07f, 691.19f, 691.31f, 691.43f, 691.55f, 691.66f, 691.78f, 691.90f, 692.02f, 692.14f, 692.26f, 692.38f, 692.50f, 692.62f, 692.73f, 692.85f, 692.97f, 693.09f, 693.21f, 693.33f, 693.45f, 693.57f, 693.69f, 693.80f, 693.92f, 694.04f, 694.16f, 694.28f, 694.40f, 694.52f, 694.64f, 694.75f, 694.87f, 694.99f, 695.11f, 695.23f, 695.35f, 695.47f, 695.59f, 695.71f, 695.82f, 695.94f, 696.06f, 696.18f, 696.30f, 696.42f, 696.54f, 696.66f, 696.78f, 696.89f, 697.01f, 697.13f, 697.25f, 697.37f, 697.49f, 697.61f, 697.73f, 697.85f, 697.96f, 698.08f, 698.20f, 698.32f, 698.44f, 698.56f, 698.68f, 698.80f, 698.92f, 699.03f, 699.15f, 699.27f, 699.39f, 699.51f, 699.63f, 699.75f, 699.87f, 699.98f, 700.10f, 700.22f, 700.34f, 700.46f, 700.58f, 700.70f, 700.82f, 700.94f, 701.05f, 701.17f, 701.29f, 701.41f, 701.53f, 701.65f, 701.77f
		398.10f, 398.44f, 398.77f, 399.11f, 399.44f, 399.77f, 400.11f, 400.44f, 400.78f, 401.11f, 401.45f, 401.78f, 402.11f, 402.45f, 402.78f, 403.12f, 403.45f, 403.79f, 404.12f, 404.46f, 404.79f, 405.13f, 405.46f, 405.80f, 406.13f, 406.47f, 406.80f, 407.14f, 407.47f, 407.81f, 408.14f, 408.48f, 408.81f, 409.15f, 409.48f, 409.82f, 410.16f, 410.49f, 410.83f, 411.16f, 411.50f, 411.83f, 412.17f, 412.51f, 412.84f, 413.18f, 413.52f, 413.85f, 414.19f, 414.52f, 414.86f, 415.20f, 415.53f, 415.87f, 416.21f, 416.54f, 416.88f, 417.22f, 417.55f, 417.89f, 418.23f, 418.56f, 418.90f, 419.24f, 419.57f, 419.91f, 420.25f, 420.59f, 420.92f, 421.26f, 421.60f, 421.94f, 422.27f, 422.61f, 422.95f, 423.29f, 423.62f, 423.96f, 424.30f, 424.64f, 424.97f, 425.31f, 425.65f, 425.99f, 426.33f, 426.66f, 427.00f, 427.34f, 427.68f, 428.02f, 428.35f, 428.69f, 429.03f, 429.37f, 429.71f, 430.05f, 430.39f, 430.72f, 431.06f, 431.40f, 431.74f, 432.08f, 432.42f, 432.76f, 433.10f, 433.43f, 433.77f, 434.11f, 434.45f, 434.79f, 435.13f, 435.47f, 435.81f, 436.15f, 436.49f, 436.83f, 437.17f, 437.51f, 437.85f, 438.19f, 438.52f, 438.86f, 439.20f, 439.54f, 439.88f, 440.22f, 440.56f, 440.90f, 441.24f, 441.58f, 441.92f, 442.26f, 442.60f, 442.94f, 443.29f, 443.63f, 443.97f, 444.31f, 444.65f, 444.99f, 445.33f, 445.67f, 446.01f, 446.35f, 446.69f, 447.03f, 447.37f, 447.71f, 448.05f, 448.39f, 448.74f, 449.08f, 449.42f, 449.76f, 450.10f, 450.44f, 450.78f, 451.12f, 451.47f, 451.81f, 452.15f, 452.49f, 452.83f, 453.17f, 453.51f, 453.86f, 454.20f, 454.54f, 454.88f, 455.22f, 455.57f, 455.91f, 456.25f, 456.59f, 456.93f, 457.28f, 457.62f, 457.96f, 458.30f, 458.64f, 458.99f, 459.33f, 459.67f, 460.01f, 460.36f, 460.70f, 461.04f, 461.38f, 461.73f, 462.07f, 462.41f, 462.75f, 463.10f, 463.44f, 463.78f, 464.13f, 464.47f, 464.81f, 465.15f, 465.50f, 465.84f, 466.18f, 466.53f, 466.87f, 467.21f, 467.56f, 467.90f, 468.24f, 468.59f, 468.93f, 469.27f, 469.62f, 469.96f, 470.30f, 470.65f, 470.99f, 471.34f, 471.68f, 472.02f, 472.37f, 472.71f, 473.06f, 473.40f, 473.74f, 474.09f, 474.43f, 474.78f, 475.12f, 475.46f, 475.81f, 476.15f, 476.50f, 476.84f, 477.19f, 477.53f, 477.87f, 478.22f, 478.56f, 478.91f, 479.25f, 479.60f, 479.94f, 480.29f, 480.63f, 480.98f, 481.32f, 481.67f, 482.01f, 482.36f, 482.70f, 483.05f, 483.39f, 483.74f, 484.08f, 484.43f, 484.77f, 485.12f, 485.46f, 485.81f, 486.15f, 486.50f, 486.84f, 487.19f, 487.54f, 487.88f, 488.23f, 488.57f, 488.92f, 489.26f, 489.61f, 489.96f, 490.30f, 490.65f, 490.99f, 491.34f, 491.68f, 492.03f, 492.38f, 492.72f, 493.07f, 493.41f, 493.76f, 494.11f, 494.45f, 494.80f, 495.15f, 495.49f, 495.84f, 496.19f, 496.53f, 496.88f, 497.22f, 497.57f, 497.92f, 498.26f, 498.61f, 498.96f, 499.30f, 499.65f, 500.00f, 500.35f, 500.69f, 501.04f, 501.39f, 501.73f, 502.08f, 502.43f, 502.77f, 503.12f, 503.47f, 503.82f, 504.16f, 504.51f, 504.86f, 505.20f, 505.55f, 505.90f, 506.25f, 506.59f, 506.94f, 507.29f, 507.64f, 507.98f, 508.33f, 508.68f, 509.03f, 509.38f, 509.72f, 510.07f, 510.42f, 510.77f, 511.11f, 511.46f, 511.81f, 512.16f, 512.51f, 512.85f, 513.20f, 513.55f, 513.90f, 514.25f, 514.60f, 514.94f, 515.29f, 515.64f, 515.99f, 516.34f, 516.69f, 517.03f, 517.38f, 517.73f, 518.08f, 518.43f, 518.78f, 519.12f, 519.47f, 519.82f, 520.17f, 520.52f, 520.87f, 521.22f, 521.57f, 521.91f, 522.26f, 522.61f, 522.96f, 523.31f, 523.66f, 524.01f, 524.36f, 524.71f, 525.06f, 525.41f, 525.75f, 526.10f, 526.45f, 526.80f, 527.15f, 527.50f, 527.85f, 528.20f, 528.55f, 528.90f, 529.25f, 529.60f, 529.95f, 530.30f, 530.65f, 531.00f, 531.35f, 531.70f, 532.05f, 532.39f, 532.74f, 533.09f, 533.44f, 533.79f, 534.14f, 534.49f, 534.84f, 535.19f, 535.54f, 535.89f, 536.24f, 536.59f, 536.94f, 537.29f, 537.64f, 537.99f, 538.35f, 538.70f, 539.05f, 539.40f, 539.75f, 540.10f, 540.45f, 540.80f, 541.15f, 541.50f, 541.85f, 542.20f, 542.55f, 542.90f, 543.25f, 543.60f, 543.95f, 544.30f, 544.65f, 545.00f, 545.36f, 545.71f, 546.06f, 546.41f, 546.76f, 547.11f, 547.46f, 547.81f, 548.16f, 548.51f, 548.86f, 549.22f, 549.57f, 549.92f, 550.27f, 550.62f, 550.97f, 551.32f, 551.67f, 552.03f, 552.38f, 552.73f, 553.08f, 553.43f, 553.78f, 554.13f, 554.48f, 554.84f, 555.19f, 555.54f, 555.89f, 556.24f, 556.59f, 556.95f, 557.30f, 557.65f, 558.00f, 558.35f, 558.70f, 559.06f, 559.41f, 559.76f, 560.11f, 560.46f, 560.82f, 561.17f, 561.52f, 561.87f, 562.22f, 562.58f, 562.93f, 563.28f, 563.63f, 563.98f, 564.34f, 564.69f, 565.04f, 565.39f, 565.74f, 566.10f, 566.45f, 566.80f, 567.15f, 567.51f, 567.86f, 568.21f, 568.56f, 568.92f, 569.27f, 569.62f, 569.97f, 570.33f, 570.68f, 571.03f, 571.38f, 571.74f, 572.09f, 572.44f, 572.79f, 573.15f, 573.50f, 573.85f, 574.20f, 574.56f, 574.91f, 575.26f, 575.62f, 575.97f, 576.32f, 576.67f, 577.03f, 577.38f, 577.73f, 578.09f, 578.44f, 578.79f, 579.15f, 579.50f, 579.85f, 580.20f, 580.56f, 580.91f, 581.26f, 581.62f, 581.97f, 582.32f, 582.68f, 583.03f, 583.38f, 583.74f, 584.09f, 584.44f, 584.80f, 585.15f, 585.50f, 585.86f, 586.21f, 586.56f, 586.92f, 587.27f, 587.62f, 587.98f, 588.33f, 588.69f, 589.04f, 589.39f, 589.75f, 590.10f, 590.45f, 590.81f, 591.16f, 591.51f, 591.87f, 592.22f, 592.58f, 592.93f, 593.28f, 593.64f, 593.99f, 594.35f, 594.70f, 595.05f, 595.41f, 595.76f, 596.11f, 596.47f, 596.82f, 597.18f, 597.53f, 597.89f, 598.24f, 598.59f, 598.95f, 599.30f, 599.66f, 600.01f, 600.36f, 600.72f, 601.07f, 601.43f, 601.78f, 602.14f, 602.49f, 602.84f, 603.20f, 603.55f, 603.91f, 604.26f, 604.62f, 604.97f, 605.32f, 605.68f, 606.03f, 606.39f, 606.74f, 607.10f, 607.45f, 607.81f, 608.16f, 608.51f, 608.87f, 609.22f, 609.58f, 609.93f, 610.29f, 610.64f, 611.00f, 611.35f, 611.71f, 612.06f, 612.42f, 612.77f, 613.13f, 613.48f, 613.84f, 614.19f, 614.54f, 614.90f, 615.25f, 615.61f, 615.96f, 616.32f, 616.67f, 617.03f, 617.38f, 617.74f, 618.09f, 618.45f, 618.80f, 619.16f, 619.51f, 619.87f, 620.22f, 620.58f, 620.93f, 621.29f, 621.64f, 622.00f, 622.35f, 622.71f, 623.06f, 623.42f, 623.78f, 624.13f, 624.49f, 624.84f, 625.20f, 625.55f, 625.91f, 626.26f, 626.62f, 626.97f, 627.33f, 627.68f, 628.04f, 628.39f, 628.75f, 629.10f, 629.46f, 629.82f, 630.17f, 630.53f, 630.88f, 631.24f, 631.59f, 631.95f, 632.30f, 632.66f, 633.01f, 633.37f, 633.73f, 634.08f, 634.44f, 634.79f, 635.15f, 635.50f, 635.86f, 636.21f, 636.57f, 636.93f, 637.28f, 637.64f, 637.99f, 638.35f, 638.70f, 639.06f, 639.42f, 639.77f, 640.13f, 640.48f, 640.84f, 641.19f, 641.55f, 641.91f, 642.26f, 642.62f, 642.97f, 643.33f, 643.68f, 644.04f, 644.40f, 644.75f, 645.11f, 645.46f, 645.82f, 646.18f, 646.53f, 646.89f, 647.24f, 647.60f, 647.95f, 648.31f, 648.67f, 649.02f, 649.38f, 649.73f, 650.09f, 650.45f, 650.80f, 651.16f, 651.51f, 651.87f, 652.23f, 652.58f, 652.94f, 653.29f, 653.65f, 654.01f, 654.36f, 654.72f, 655.08f, 655.43f, 655.79f, 656.14f, 656.50f, 656.86f, 657.21f, 657.57f, 657.92f, 658.28f, 658.64f, 658.99f, 659.35f, 659.71f, 660.06f, 660.42f, 660.77f, 661.13f, 661.49f, 661.84f, 662.20f, 662.55f, 662.91f, 663.27f, 663.62f, 663.98f, 664.34f, 664.69f, 665.05f, 665.41f, 665.76f, 666.12f, 666.47f, 666.83f, 667.19f, 667.54f, 667.90f, 668.26f, 668.61f, 668.97f, 669.32f, 669.68f, 670.04f, 670.39f, 670.75f, 671.11f, 671.46f, 671.82f, 672.18f, 672.53f, 672.89f, 673.24f, 673.60f, 673.96f, 674.31f, 674.67f, 675.03f, 675.38f, 675.74f, 676.10f, 676.45f, 676.81f, 677.17f, 677.52f, 677.88f, 678.24f, 678.59f, 678.95f, 679.30f, 679.66f, 680.02f, 680.37f, 680.73f, 681.09f, 681.44f, 681.80f, 682.16f, 682.51f, 682.87f, 683.23f, 683.58f, 683.94f, 684.30f, 684.65f, 685.01f, 685.37f, 685.72f, 686.08f, 686.43f, 686.79f, 687.15f, 687.50f, 687.86f, 688.22f, 688.57f, 688.93f, 689.29f, 689.64f, 690.00f, 690.36f, 690.71f, 691.07f, 691.43f, 691.78f, 692.14f, 692.50f, 692.85f, 693.21f, 693.57f, 693.92f, 694.28f, 694.64f, 694.99f, 695.35f, 695.71f, 696.06f, 696.42f, 696.78f, 697.13f, 697.49f, 697.85f, 698.20f, 698.56f, 698.92f, 699.27f, 699.63f, 699.98f, 700.34f, 700.70f, 701.05f, 701.41f, 701.77f
	)
	val vetIThor = listOf(
	//	0.028f, -0.041f, 0.026f, -0.039f, 0.030f, -0.042f, 0.028f, -0.038f, 0.029f, -0.043f, 0.027f, -0.043f, 0.030f, -0.042f, 0.027f, -0.038f, 0.027f, -0.041f, 0.030f, -0.040f, 0.031f, -0.039f, 0.033f, -0.039f, 0.032f, -0.042f, 0.031f, -0.039f, 0.031f, -0.037f, 0.026f, -0.041f, 0.030f, -0.037f, 0.030f, -0.040f, 0.032f, -0.041f, 0.028f, -0.040f, 0.029f, -0.038f, 0.030f, -0.039f, 0.030f, -0.037f, 0.029f, -0.039f, 0.030f, -0.040f, 0.029f, -0.040f, 0.032f, -0.037f, 0.029f, -0.039f, 0.032f, -0.036f, 0.032f, -0.037f, 0.031f, -0.041f, 0.032f, -0.038f, 0.033f, -0.038f, 0.032f, -0.036f, 0.030f, -0.035f, 0.032f, -0.036f, 0.033f, -0.036f, 0.030f, -0.036f, 0.032f, -0.036f, 0.033f, -0.037f, 0.034f, -0.035f, 0.033f, -0.036f, 0.036f, -0.035f, 0.036f, -0.035f, 0.038f, -0.031f, 0.035f, -0.035f, 0.036f, -0.034f, 0.037f, -0.030f, 0.036f, -0.033f, 0.038f, -0.031f, 0.039f, -0.030f, 0.041f, -0.032f, 0.039f, -0.029f, 0.037f, -0.027f, 0.043f, -0.028f, 0.039f, -0.026f, 0.044f, -0.024f, 0.040f, -0.024f, 0.042f, -0.027f, 0.045f, -0.023f, 0.045f, -0.023f, 0.047f, -0.026f, 0.046f, -0.022f, 0.048f, -0.022f, 0.048f, -0.019f, 0.051f, -0.016f, 0.055f, -0.018f, 0.053f, -0.015f, 0.054f, -0.011f, 0.058f, -0.010f, 0.056f, -0.011f, 0.059f, -0.010f, 0.060f, -0.009f, 0.063f, -0.007f, 0.064f, -0.006f, 0.067f, 0.001f, 0.067f, 0.003f, 0.069f, 0.005f, 0.074f, 0.005f, 0.076f, 0.011f, 0.080f, 0.013f, 0.082f, 0.012f, 0.082f, 0.017f, 0.087f, 0.022f, 0.089f, 0.024f, 0.095f, 0.028f, 0.096f, 0.030f, 0.102f, 0.033f, 0.109f, 0.037f, 0.111f, 0.039f, 0.111f, 0.046f, 0.118f, 0.050f, 0.122f, 0.058f, 0.128f, 0.062f, 0.133f, 0.066f, 0.139f, 0.071f, 0.141f, 0.079f, 0.151f, 0.083f, 0.156f, 0.091f, 0.166f, 0.097f, 0.169f, 0.106f, 0.179f, 0.110f, 0.185f, 0.118f, 0.192f, 0.128f, 0.200f, 0.136f, 0.210f, 0.146f, 0.216f, 0.153f, 0.226f, 0.165f, 0.237f, 0.174f, 0.246f, 0.184f, 0.259f, 0.196f, 0.271f, 0.210f, 0.281f, 0.217f, 0.297f, 0.236f, 0.306f, 0.246f, 0.316f, 0.257f, 0.329f, 0.269f, 0.343f, 0.284f, 0.359f, 0.298f, 0.374f, 0.313f, 0.392f, 0.332f, 0.407f, 0.343f, 0.423f, 0.361f, 0.440f, 0.379f, 0.456f, 0.390f, 0.468f, 0.413f, 0.491f, 0.435f, 0.511f, 0.457f, 0.526f, 0.468f, 0.549f, 0.490f, 0.569f, 0.507f, 0.586f, 0.519f, 0.652f, 0.563f, 0.639f, 0.583f, 0.664f, 0.606f, 0.682f, 0.634f, 0.706f, 0.651f, 0.734f, 0.682f, 0.761f, 0.706f, 0.795f, 0.734f, 0.820f, 0.768f, 0.848f, 0.794f, 0.870f, 0.821f, 0.910f, 0.856f, 0.940f, 0.882f, 0.967f, 0.923f, 0.950f, 1.039f, 0.991f, 1.068f, 1.024f, 1.106f, 1.060f, 1.140f, 1.093f, 1.182f, 1.142f, 1.226f, 1.181f, 1.267f, 1.220f, 1.309f, 1.257f, 1.349f, 1.309f, 1.402f, 1.352f, 1.434f, 1.392f, 1.487f, 1.447f, 1.531f, 1.486f, 1.580f, 1.545f, 1.627f, 1.586f, 1.686f, 1.651f, 1.755f, 1.702f, 1.794f, 1.754f, 1.852f, 1.811f, 1.902f, 1.858f, 1.962f, 1.920f, 2.013f, 1.967f, 2.064f, 2.046f, 2.140f, 2.096f, 2.193f, 2.169f, 2.272f, 2.235f, 2.320f, 2.291f, 2.399f, 2.372f, 2.452f, 2.423f, 2.553f, 2.497f, 2.588f, 2.537f, 2.659f, 2.659f, 2.756f, 2.722f, 2.825f, 2.813f, 2.938f, 2.876f, 2.975f, 2.958f, 3.073f, 3.038f, 3.149f, 3.125f, 3.242f, 3.203f, 3.303f, 3.279f, 3.418f, 3.405f, 3.501f, 3.471f, 3.623f, 3.610f, 3.736f, 3.693f, 3.833f, 3.825f, 3.953f, 3.935f, 4.044f, 4.046f, 4.207f, 4.176f, 4.294f, 4.296f, 4.461f, 4.469f, 4.600f, 4.583f, 4.717f, 4.713f, 4.852f, 4.847f, 5.004f, 5.019f, 5.144f, 5.122f, 5.263f, 5.295f, 5.457f, 5.453f, 5.603f, 5.609f, 5.778f, 5.810f, 5.949f, 5.948f, 6.109f, 6.149f, 6.324f, 6.333f, 6.489f, 6.465f, 6.665f, 6.717f, 6.847f, 6.831f, 7.072f, 7.129f, 7.274f, 7.274f, 7.429f, 7.487f, 7.659f, 7.611f, 7.811f, 7.877f, 8.037f, 8.038f, 8.197f, 8.218f, 8.386f, 8.396f, 8.559f, 8.524f, 8.775f, 8.777f, 8.921f, 8.853f, 9.013f, 9.100f, 9.240f, 9.183f, 9.314f, 9.336f, 9.561f, 9.551f, 9.618f, 9.623f, 9.778f, 9.816f, 9.923f, 9.846f, 10.019f, 10.043f, 10.144f, 10.047f, 10.128f, 10.117f, 10.182f, 10.019f, 10.110f, 10.102f, 10.167f, 10.055f, 10.117f, 10.050f, 10.166f, 10.074f, 10.144f, 9.996f, 10.024f, 9.969f, 9.974f, 9.803f, 9.864f, 9.790f, 9.835f, 9.697f, 9.662f, 9.513f, 9.527f, 9.448f, 9.436f, 9.230f, 9.192f, 9.045f, 9.025f, 8.914f, 8.930f, 8.803f, 8.769f, 8.552f, 8.515f, 8.348f, 8.382f, 8.214f, 8.131f, 8.005f, 7.996f, 7.821f, 7.770f, 7.585f, 7.534f, 7.337f, 7.310f, 7.168f, 7.132f, 6.957f, 6.944f, 6.813f, 6.825f, 6.664f, 6.598f, 6.400f, 6.371f, 6.224f, 6.213f, 6.047f, 6.014f, 5.864f, 5.842f, 5.683f, 5.681f, 5.511f, 5.471f, 5.324f, 5.357f, 5.267f, 5.268f, 5.105f, 5.116f, 5.018f, 5.013f, 4.845f, 4.862f, 4.746f, 4.771f, 4.639f, 4.621f, 4.505f, 4.566f, 4.457f, 4.325f, 4.335f, 4.254f, 4.295f, 4.155f, 4.141f, 4.049f, 4.094f, 3.999f, 4.004f, 3.893f, 3.935f, 3.835f, 3.861f, 3.733f, 3.780f, 3.724f, 3.779f, 3.676f, 3.695f, 3.615f, 3.678f, 3.564f, 3.598f, 3.516f, 3.557f, 3.486f, 3.538f, 3.469f, 3.505f, 3.418f, 3.462f, 3.351f, 3.394f, 3.331f, 3.402f, 3.307f, 3.354f, 3.264f, 3.313f, 3.238f, 3.278f, 3.206f, 3.264f, 3.191f, 3.254f, 3.185f, 3.219f, 3.144f, 3.194f, 3.112f, 3.159f, 3.067f, 3.133f, 3.058f, 3.123f, 3.024f, 3.077f, 2.983f, 3.048f, 2.982f, 3.017f, 2.931f, 2.991f, 2.880f, 2.929f, 2.838f, 2.882f, 2.807f, 2.875f, 2.764f, 2.790f, 2.699f, 2.770f, 2.678f, 2.724f, 2.650f, 2.683f, 2.600f, 2.647f, 2.548f, 2.598f, 2.524f, 2.575f, 2.479f, 2.521f, 2.430f, 2.486f, 2.397f, 2.426f, 2.336f, 2.381f, 2.301f, 2.360f, 2.278f, 2.334f, 2.244f, 2.280f, 2.182f, 2.220f, 2.135f, 2.197f, 2.113f, 2.162f, 2.075f, 2.113f, 2.025f, 2.070f, 1.990f, 2.046f, 1.964f, 2.012f, 1.914f, 1.950f, 1.875f, 1.941f, 1.855f, 1.886f, 1.795f, 1.853f, 1.780f, 1.837f, 1.752f, 1.792f, 1.716f, 1.773f, 1.687f, 1.729f, 1.639f, 1.711f, 1.628f, 1.678f, 1.588f, 1.661f, 1.592f, 1.627f, 1.538f, 1.598f, 1.524f, 1.585f, 1.504f, 1.553f, 1.479f, 1.541f, 1.469f, 1.521f, 1.440f, 1.511f, 1.433f, 1.501f, 1.426f, 1.478f, 1.406f, 1.460f, 1.382f, 1.440f, 1.369f, 1.443f, 1.365f, 1.420f, 1.342f, 1.398f, 1.337f, 1.412f, 1.336f, 1.390f, 1.322f, 1.387f, 1.321f, 1.375f, 1.296f, 1.364f, 1.310f, 1.368f, 1.294f, 1.351f, 1.279f, 1.353f, 1.278f, 1.348f, 1.278f, 1.340f, 1.282f, 1.348f, 1.275f, 1.342f, 1.272f, 1.338f, 1.265f, 1.327f, 1.266f, 1.334f, 1.275f, 1.334f, 1.255f, 1.316f, 1.254f, 1.325f, 1.256f, 1.316f, 1.257f, 1.327f, 1.255f, 1.314f, 1.237f, 1.309f, 1.249f, 1.312f, 1.240f, 1.312f, 1.241f, 1.307f, 1.235f, 1.295f, 1.229f, 1.301f, 1.239f, 1.300f, 1.224f, 1.292f, 1.227f, 1.298f, 1.227f, 1.294f, 1.232f, 1.296f, 1.232f, 1.290f, 1.228f, 1.293f, 1.231f, 1.291f, 1.222f, 1.290f, 1.225f, 1.293f, 1.226f, 1.280f, 1.217f, 1.289f, 1.224f, 1.299f, 1.218f, 1.288f, 1.232f, 1.305f, 1.233f, 1.305f, 1.233f, 1.305f, 1.247f, 1.308f, 1.249f, 1.315f, 1.249f, 1.250f, 1.319f, 1.255f, 1.335f, 1.268f, 1.336f, 1.270f, 1.340f, 1.280f, 1.350f, 1.284f, 1.362f, 1.307f, 1.371f, 1.306f, 1.370f, 1.320f, 1.388f, 1.334f, 1.409f, 1.343f, 1.415f, 1.357f, 1.431f, 1.373f, 1.447f, 1.383f, 1.466f, 1.397f, 1.472f, 1.411f, 1.482f, 1.429f, 1.512f, 1.459f, 1.530f, 1.472f, 1.546f, 1.492f, 1.564f, 1.502f, 1.581f, 1.534f, 1.605f, 1.548f, 1.616f, 1.557f, 1.644f, 1.593f, 1.665f, 1.605f, 1.683f, 1.633f, 1.716f, 1.647f, 1.721f, 1.677f, 1.767f, 1.711f, 1.786f, 1.721f, 1.807f, 1.760f, 1.844f, 1.792f, 1.859f, 1.809f, 1.892f, 1.838f, 1.918f, 1.862f, 1.941f, 1.902f, 1.989f, 1.933f, 1.996f, 1.942f, 2.038f, 1.986f, 2.057f, 2.011f, 2.093f, 2.053f, 2.125f, 2.063f, 2.135f, 2.095f, 2.189f, 2.146f, 2.218f, 2.161f, 2.245f, 2.195f, 2.278f, 2.230f, 2.298f, 2.262f, 2.354f, 2.305f, 2.367f, 2.313f, 2.389f, 2.360f, 2.445f, 2.395f, 2.472f, 2.434f, 2.524f, 2.454f, 2.531f, 2.483f, 2.565f, 2.532f, 2.635f, 2.572f, 2.639f, 2.587f, 2.684f, 2.644f, 2.726f, 2.668f, 2.755f, 2.717f, 2.814f, 2.753f, 2.832f, 2.775f, 2.876f, 2.841f, 2.928f, 2.864f, 2.950f, 2.910f, 2.991f, 2.937f, 3.017f, 2.976f, 3.083f, 3.046f, 3.108f, 3.049f, 3.134f, 3.099f, 3.191f, 3.150f, 3.225f, 3.178f, 3.275f, 3.235f, 3.313f, 3.256f, 3.338f, 3.306f, 3.408f, 3.347f, 3.422f, 3.387f, 3.480f, 3.434f, 3.514f, 3.463f, 3.538f, 3.513f, 3.623f, 3.559f, 3.632f, 3.583f, 3.677f, 3.642f, 3.742f, 3.686f, 3.750f, 3.728f, 3.834f, 3.786f, 3.854f, 3.791f, 3.884f, 3.862f, 3.957f, 3.894f, 3.988f, 3.946f, 4.045f, 4.003f, 4.068f, 4.004f, 4.101f, 4.086f, 4.180f, 4.103f, 4.165f, 4.114f, 4.209f, 4.183f, 4.290f, 4.223f, 4.323f, 4.304f, 4.393f, 4.327f, 4.396f, 4.343f, 4.428f, 4.410f, 4.498f, 4.429f, 4.523f, 4.491f, 4.587f, 4.537f, 4.603f, 4.544f, 4.653f, 4.629f, 4.721f, 4.647f, 4.722f, 4.686f, 4.766f, 4.730f, 4.808f, 4.754f, 4.846f, 4.823f, 4.911f, 4.845f, 4.918f, 4.856f, 4.948f, 4.927f, 5.013f, 4.943f, 5.016f, 5.008f, 5.117f, 5.050f, 5.112f, 5.043f, 5.125f, 5.109f, 5.210f, 5.135f, 5.208f, 5.173f, 5.270f, 5.214f, 5.277f, 5.220f, 5.298f, 5.268f, 5.376f, 5.308f, 5.363f, 5.317f, 5.410f, 5.452f, 5.379f, 5.447f, 5.409f, 5.534f, 5.468f, 5.525f, 5.461f, 5.544f, 5.515f, 5.587f, 5.532f, 5.593f, 5.560f, 5.660f, 5.624f, 5.675f, 5.605f, 5.667f, 5.626f, 5.724f, 5.665f, 5.728f, 5.669f, 5.775f, 5.725f, 5.775f, 5.702f, 5.784f, 5.741f, 5.837f, 5.788f, 5.848f, 5.768f, 5.863f, 5.818f, 5.904f, 5.835f, 5.890f, 5.818f, 5.916f, 5.878f, 5.931f, 5.854f, 5.943f, 5.918f, 5.976f, 5.908f, 5.964f, 5.893f, 5.984f, 5.969f, 6.043f, 5.955f, 6.014f, 5.975f, 6.043f, 5.963f, 6.027f, 5.955f, 6.036f, 6.009f, 6.099f, 6.025f, 6.069f, 6.000f, 6.095f, 6.038f, 6.110f, 6.023f, 6.077f, 6.038f, 6.142f, 6.077f, 6.122f, 6.040f, 6.135f, 6.088f, 6.158f, 6.085f, 6.112f, 6.050f, 6.159f, 6.140f, 6.199f, 6.096f, 6.167f, 6.107f, 6.187f, 6.129f, 6.179f, 6.096f, 6.194f, 6.182f, 6.248f, 6.151f, 6.191f, 6.124f, 6.222f, 6.193f, 6.238f, 6.153f, 6.214f, 6.186f, 6.277f, 6.213f, 6.228f, 6.154f, 6.248f, 6.217f, 6.292f, 6.215f, 6.255f, 6.185f, 6.275f, 6.255f, 6.290f, 6.218f, 6.276f, 6.242f, 6.314f, 6.266f, 6.312f, 6.223f, 6.282f, 6.293f, 6.374f, 6.291f, 6.315f, 6.243f, 6.299f, 6.246f, 6.322f, 6.245f, 6.285f, 6.246f, 6.375f, 6.346f, 6.362f, 6.271f, 6.320f, 6.290f, 6.393f, 6.334f, 6.363f, 6.286f, 6.376f, 6.348f, 6.395f, 6.307f, 6.357f, 6.312f, 6.399f, 6.369f, 6.415f, 6.324f, 6.382f, 6.349f, 6.441f, 6.360f, 6.398f, 6.354f, 6.429f, 6.379f, 6.438f, 6.359f, 6.399f, 6.360f, 6.449f, 6.407f, 6.456f, 6.378f, 6.441f, 6.415f, 6.479f, 6.381f, 6.425f, 6.356f, 6.441f, 6.400f, 6.469f, 6.397f, 6.433f, 6.379f, 6.463f, 6.414f, 6.461f, 6.361f, 6.409f, 6.396f, 6.472f, 6.401f, 6.445f, 6.361f, 6.417f, 6.405f, 6.475f, 6.392f, 6.397f, 6.338f, 6.481f, 6.413f, 6.475f, 6.387f, 6.413f, 6.354f, 6.468f, 6.438f, 6.473f, 6.365f, 6.433f, 6.409f, 6.495f, 6.416f, 6.462f, 6.351f, 6.402f, 6.392f, 6.487f, 6.408f, 6.437f, 6.370f, 6.463f, 6.433f, 6.480f, 6.394f, 6.434f, 6.353f, 6.439f, 6.424f, 6.480f, 6.388f, 6.432f, 6.423f, 6.521f, 6.446f, 6.477f, 6.395f, 6.436f, 6.392f, 6.475f, 6.425f, 6.450f, 6.390f, 6.478f, 6.476f, 6.527f, 6.427f, 6.469f, 6.412f, 6.482f, 6.438f, 6.495f, 6.432f, 6.477f, 6.433f, 6.486f, 6.494f, 6.388f, 6.499f, 6.464f, 6.533f, 6.445f, 6.486f, 6.439f, 6.517f, 6.487f, 6.545f, 6.453f, 6.461f, 6.410f, 6.526f, 6.486f, 6.527f, 6.417f, 6.496f, 6.472f, 6.564f, 6.490f, 6.531f, 6.443f, 6.469f, 6.443f, 6.547f, 6.502f, 6.514f, 6.418f, 6.521f, 6.511f, 6.563f, 6.451f, 6.478f, 6.417f, 6.490f, 6.466f, 6.509f, 6.436f, 6.455f, 6.389f, 6.498f, 6.474f, 6.527f, 6.430f, 6.481f, 6.441f, 6.531f, 6.459f, 6.480f, 6.419f, 6.461f, 6.405f, 6.477f, 6.438f, 6.486f, 6.400f, 6.461f, 6.456f, 6.530f, 6.435f, 6.455f, 6.396f, 6.484f, 6.402f, 6.441f, 6.376f, 6.433f, 6.379f, 6.446f, 6.384f, 6.430f, 6.347f, 6.400f, 6.382f, 6.478f, 6.408f, 6.401f, 6.303f, 6.383f, 6.376f, 6.426f, 6.332f, 6.350f, 6.274f, 6.365f, 6.346f, 6.425f, 6.343f, 6.347f, 6.285f, 6.379f, 6.365f, 6.404f, 6.304f, 6.338f, 6.268f, 6.359f, 6.353f, 6.364f, 6.270f, 6.302f, 6.250f, 6.343f, 6.306f, 6.346f, 6.243f, 6.294f, 6.262f, 6.361f, 6.294f, 6.303f, 6.221f, 6.260f, 6.214f, 6.301f, 6.220f, 6.265f, 6.200f, 6.248f, 6.212f, 6.310f, 6.238f, 6.259f, 6.183f, 6.274f, 6.267f, 6.318f, 6.209f, 6.216f, 6.171f, 6.276f, 6.213f, 6.261f, 6.168f, 6.214f, 6.184f, 6.265f, 6.219f, 6.248f, 6.158f, 6.204f, 6.165f, 6.273f, 6.216f, 6.248f, 6.149f, 6.202f, 6.163f, 6.269f, 6.191f, 6.205f, 6.098f, 6.134f, 6.112f, 6.225f, 6.130f, 6.149f, 6.048f, 6.116f, 6.063f, 6.159f, 6.077f, 6.093f, 6.012f, 6.079f, 6.063f, 6.118f, 6.032f, 6.033f, 5.966f, 6.056f, 5.996f, 6.070f, 5.977f, 6.014f, 5.940f, 6.013f, 5.943f, 6.000f, 5.911f, 5.955f, 5.911f, 6.008f, 5.940f, 5.984f, 5.886f, 5.924f, 5.876f, 5.967f, 5.916f, 5.942f, 5.857f, 5.904f, 5.868f, 5.947f, 5.876f, 5.895f, 5.793f, 5.840f, 5.808f, 5.872f, 5.823f, 5.834f, 5.738f, 5.758f, 5.714f, 5.803f, 5.751f, 5.767f, 5.659f, 5.725f, 5.679f, 5.779f, 5.700f, 5.725f, 5.621f, 5.681f, 5.640f, 5.709f, 5.642f, 5.656f, 5.572f, 5.632f, 5.583f, 5.630f, 5.571f, 5.591f, 5.526f, 5.581f, 5.562f, 5.627f, 5.531f, 5.568f, 5.502f, 5.572f, 5.580f, 5.635f, 5.543f, 5.547f, 5.486f, 5.576f, 5.559f, 5.599f, 5.490f, 5.525f, 5.483f, 5.551f, 5.519f, 5.559f, 5.463f, 5.490f, 5.421f, 5.512f, 5.538f, 5.439f, 5.470f, 5.397f, 5.459f, 5.451f, 5.496f, 5.396f, 5.446f, 5.378f, 5.461f, 5.410f, 5.448f, 5.349f, 5.392f, 5.362f, 5.447f, 5.395f, 5.428f, 5.338f, 5.372f, 5.329f, 5.402f, 5.334f, 5.399f, 5.299f, 5.341f, 5.301f, 5.393f, 5.327f, 5.340f, 5.238f, 5.299f, 5.235f, 5.337f, 5.284f, 5.335f, 5.221f, 5.256f, 5.207f, 5.296f, 5.244f, 5.293f, 5.202f, 5.232f, 5.171f, 5.250f, 5.215f, 5.249f, 5.152f, 5.198f, 5.152f, 5.233f, 5.179f, 5.222f, 5.132f, 5.164f, 5.115f, 5.198f, 5.136f, 5.171f, 5.066f, 5.133f, 5.089f, 5.181f, 5.086f, 5.135f, 5.043f, 5.096f, 5.068f, 5.153f, 5.079f, 5.110f, 5.009f, 5.063f, 5.029f, 5.116f, 5.065f, 5.104f, 5.010f, 5.052f, 4.985f, 5.079f, 5.041f, 5.078f, 4.990f, 5.033f, 4.975f, 5.047f, 4.979f, 5.030f, 4.959f, 5.000f, 4.957f, 5.039f, 4.981f, 5.020f, 4.920f, 4.967f, 4.918f, 5.016f, 4.963f, 5.012f, 4.931f, 4.964f, 4.901f, 5.006f, 4.956f, 5.014f, 4.921f, 4.953f, 4.894f, 4.978f, 4.947f, 4.997f, 4.904f, 4.947f, 4.874f, 4.969f, 4.936f, 4.996f, 4.906f, 4.940f, 4.879f, 4.953f, 4.909f, 4.968f, 4.889f, 4.932f, 4.860f, 4.936f, 4.892f, 4.942f, 4.861f, 4.921f, 4.840f, 4.928f, 4.873f, 4.930f, 4.846f, 4.886f, 4.809f, 4.887f, 4.855f, 4.927f, 4.839f, 4.858f, 4.788f, 4.860f, 4.820f, 4.902f, 4.813f, 4.849f, 4.757f, 4.823f, 4.777f, 4.862f, 4.796f, 4.815f, 4.746f, 4.793f, 4.741f, 4.826f, 4.753f, 4.796f, 4.711f, 4.770f, 4.727f, 4.781f, 4.711f, 4.744f, 4.655f, 4.735f, 4.680f, 4.760f, 4.686f, 4.706f, 4.615f, 4.660f, 4.616f, 4.693f, 4.630f, 4.651f, 4.552f, 4.589f, 4.541f, 4.616f, 4.586f, 4.635f, 4.558f, 4.577f, 4.503f, 4.570f, 4.510f, 4.564f, 4.493f, 4.536f, 4.470f, 4.527f, 4.481f, 4.535f, 4.441f, 4.476f, 4.409f, 4.469f, 4.425f, 4.472f, 4.389f, 4.416f, 4.331f, 4.399f, 4.358f, 4.415f, 4.350f, 4.368f, 4.280f, 4.325f, 4.281f, 4.346f, 4.279f, 4.315f, 4.225f, 4.287f, 4.218f, 4.295f, 4.225f, 4.261f, 4.183f, 4.235f, 4.186f, 4.245f, 4.183f, 4.223f, 4.118f, 4.165f, 4.113f, 4.185f, 4.131f, 4.170f, 4.075f, 4.112f, 4.044f, 4.114f, 4.077f, 4.127f, 4.051f, 4.079f, 3.998f, 4.061f, 4.007f, 4.067f, 4.003f, 4.035f, 3.953f, 4.015f, 3.958f, 3.952f, 3.985f, 3.910f, 3.965f, 3.914f, 3.971f, 3.899f, 3.930f, 3.849f, 3.897f, 3.842f, 3.922f, 3.850f, 3.903f, 3.799f, 3.842f, 3.787f, 3.847f, 3.792f, 3.850f, 3.760f, 3.801f, 3.730f, 3.792f, 3.734f, 3.787f, 3.714f, 3.758f, 3.688f, 3.751f, 3.696f, 3.740f, 3.660f, 3.696f, 3.635f, 3.696f, 3.644f, 3.712f, 3.645f, 3.676f, 3.593f, 3.645f, 3.586f, 3.656f, 3.599f, 3.651f, 3.570f, 3.606f, 3.544f, 3.607f, 3.553f, 3.600f, 3.526f, 3.565f, 3.499f, 3.564f, 3.508f, 3.558f, 3.478f, 3.506f, 3.438f, 3.501f, 3.462f, 3.522f, 3.445f, 3.477f, 3.399f, 3.448f, 3.396f, 3.459f, 3.409f, 3.441f, 3.363f, 3.398f, 3.339f, 3.401f, 3.359f, 3.391f, 3.326f, 3.365f, 3.295f, 3.352f, 3.301f, 3.334f, 3.272f, 3.299f, 3.236f, 3.299f, 3.251f, 3.293f, 3.230f, 3.256f, 3.182f, 3.225f, 3.183f, 3.240f, 3.193f, 3.229f, 3.156f, 3.191f, 3.120f, 3.178f, 3.127f, 3.179f, 3.100f, 3.143f, 3.084f, 3.129f, 3.065f, 3.116f, 3.049f, 3.081f, 3.009f, 3.061f, 3.013f, 3.063f, 3.001f, 3.030f, 2.952f, 2.983f, 2.942f, 2.999f, 2.949f, 2.995f, 2.915f, 2.958f, 2.896f, 2.953f, 2.900f, 2.937f, 2.875f, 2.906f, 2.851f, 2.898f, 2.844f, 2.885f, 2.811f, 2.851f, 2.785f, 2.824f, 2.774f, 2.827f, 2.778f, 2.810f, 2.731f, 2.768f, 2.714f, 2.762f, 2.709f, 2.758f, 2.694f, 2.729f, 2.655f, 2.693f, 2.646f, 2.705f, 2.626f, 2.663f, 2.601f, 2.649f, 2.594f, 2.647f, 2.587f, 2.614f, 2.539f, 2.575f, 2.519f, 2.579f, 2.523f, 2.571f, 2.502f, 2.535f, 2.462f, 2.508f, 2.464f, 2.505f, 2.454f, 2.474f, 2.414f, 2.460f, 2.401f, 2.446f, 2.391f, 2.421f, 2.353f, 2.391f, 2.335f, 2.388f, 2.344f, 2.385f, 2.323f, 2.344f, 2.287f, 2.326f, 2.283f, 2.332f, 2.271f, 2.307f, 2.250f, 2.282f, 2.236f, 2.279f, 2.224f, 2.253f, 2.190f, 2.231f, 2.177f, 2.231f, 2.186f, 2.215f, 2.157f, 2.179f, 2.116f, 2.164f, 2.117f, 2.163f, 2.104f, 2.145f, 2.083f, 2.113f, 2.067f, 2.101f, 2.054f, 2.095f, 2.029f, 2.057f, 2.006f, 2.051f, 2.006f, 2.048f, 1.979f, 2.013f, 1.948f, 1.994f, 1.939f, 1.992f, 1.935f, 1.971f, 1.908f, 1.943f, 1.889f, 1.932f, 1.874f, 1.915f, 1.859f, 1.889f, 1.834f, 1.873f, 1.821f, 1.874f, 1.811f, 1.847f, 1.783f, 1.819f, 1.757f, 1.812f, 1.800f, 1.735f, 1.772f, 1.709f, 1.755f, 1.704f, 1.750f, 1.688f, 1.719f, 1.658f, 1.707f, 1.645f, 1.699f, 1.654f, 1.683f, 1.617f, 1.656f, 1.594f, 1.638f, 1.595f, 1.636f, 1.574f, 1.618f, 1.547f, 1.589f, 1.545f, 1.586f, 1.530f, 1.570f, 1.503f, 1.549f, 1.487f, 1.542f, 1.496f, 1.536f, 1.470f, 1.513f, 1.437f, 1.483f, 1.430f, 1.486f, 1.425f, 1.465f, 1.399f, 1.448f, 1.390f, 1.447f, 1.390f, 1.435f, 1.373f, 1.418f, 1.345f, 1.397f, 1.340f, 1.398f, 1.341f, 1.380f, 1.315f, 1.350f, 1.296f, 1.356f, 1.295f, 1.345f, 1.284f, 1.326f, 1.263f, 1.310f, 1.254f, 1.307f, 1.241f, 1.295f, 1.221f, 1.271f, 1.209f, 1.265f, 1.210f, 1.261f, 1.198f, 1.244f, 1.177f, 1.225f, 1.174f, 1.224f, 1.169f, 1.214f, 1.145f, 1.193f, 1.127f, 1.193f, 1.132f, 1.186f, 1.123f, 1.163f, 1.102f, 1.154f, 1.093f, 1.152f, 1.093f, 1.141f, 1.075f, 1.122f, 1.057f, 1.117f, 1.060f, 1.112f, 1.046f, 1.088f, 1.024f, 1.075f, 1.018f, 1.073f, 1.018f, 1.064f, 1.005f, 1.047f, 0.987f, 1.038f, 0.979f, 1.033f, 0.974f, 1.021f, 0.953f, 1.011f, 0.949f, 1.009f, 0.951f, 1.001f, 0.935f, 0.981f, 0.918f, 0.974f, 0.915f, 0.979f, 0.913f, 0.961f, 0.898f, 0.944f, 0.888f, 0.945f, 0.887f, 0.940f, 0.871f, 0.923f, 0.859f, 0.914f, 0.855f, 0.917f, 0.851f, 0.907f, 0.837f, 0.889f, 0.824f, 0.886f, 0.824f, 0.883f, 0.816f, 0.873f, 0.804f, 0.858f, 0.796f, 0.856f, 0.791f, 0.856f, 0.783f, 0.833f, 0.769f, 0.826f, 0.770f, 0.823f, 0.765f, 0.811f, 0.729f, 0.838f, 0.744f, 0.799f, 0.739f, 0.798f, 0.732f, 0.784f, 0.714f, 0.779f, 0.718f, 0.771f, 0.712f, 0.769f, 0.698f, 0.752f, 0.684f, 0.747f, 0.685f, 0.744f, 0.680f, 0.737f, 0.673f, 0.721f, 0.664f, 0.721f, 0.661f, 0.715f, 0.648f, 0.700f, 0.640f, 0.694f, 0.635f, 0.695f, 0.634f, 0.689f, 0.623f, 0.677f, 0.613f, 0.672f, 0.610f, 0.672f, 0.604f, 0.656f, 0.586f, 0.646f, 0.585f, 0.643f, 0.584f, 0.643f, 0.573f, 0.630f, 0.562f, 0.625f, 0.560f, 0.626f, 0.560f, 0.616f, 0.547f, 0.606f, 0.539f, 0.603f, 0.541f, 0.600f, 0.534f, 0.588f, 0.526f, 0.581f, 0.517f, 0.582f, 0.518f, 0.576f, 0.511f, 0.564f, 0.496f, 0.552f, 0.497f, 0.557f, 0.496f, 0.557f, 0.485f, 0.542f, 0.476f, 0.542f, 0.474f, 0.473f, 0.530f, 0.458f, 0.526f, 0.457f, 0.519f, 0.454f, 0.515f, 0.450f, 0.509f, 0.437f, 0.501f, 0.436f, 0.497f, 0.434f, 0.493f, 0.423f, 0.481f, 0.417f, 0.479f, 0.414f, 0.477f, 0.410f, 0.474f, 0.407f, 0.467f, 0.402f, 0.459f, 0.393f, 0.457f, 0.390f, 0.452f, 0.383f, 0.442f, 0.381f, 0.441f, 0.379f, 0.440f, 0.369f, 0.429f, 0.365f, 0.429f, 0.357f, 0.423f, 0.357f, 0.415f, 0.346f, 0.410f, 0.341f, 0.401f, 0.339f, 0.398f, 0.336f, 0.398f, 0.328f, 0.393f, 0.322f, 0.387f, 0.321f, 0.386f, 0.317f, 0.381f, 0.312f, 0.372f, 0.307f, 0.368f, 0.302f, 0.367f, 0.303f, 0.363f, 0.298f, 0.359f, 0.292f, 0.358f, 0.291f, 0.354f, 0.286f, 0.350f, 0.278f, 0.343f, 0.276f, 0.342f, 0.275f, 0.339f, 0.271f, 0.331f, 0.267f, 0.327f, 0.265f, 0.329f, 0.263f, 0.329f, 0.257f, 0.321f, 0.253f, 0.318f, 0.250f, 0.313f, 0.246f, 0.312f, 0.245f, 0.305f, 0.238f, 0.302f, 0.242f, 0.298f, 0.239f, 0.299f, 0.232f, 0.296f, 0.226f, 0.289f, 0.225f, 0.292f, 0.223f, 0.289f, 0.220f, 0.282f, 0.215f, 0.279f, 0.214f, 0.279f, 0.212f, 0.278f, 0.213f, 0.274f, 0.209f, 0.272f, 0.209f, 0.272f, 0.200f, 0.267f, 0.198f, 0.262f, 0.197f, 0.262f, 0.196f, 0.260f, 0.193f, 0.257f, 0.189f, 0.256f, 0.188f, 0.255f, 0.187f, 0.252f, 0.187f, 0.248f, 0.179f, 0.245f, 0.177f, 0.239f, 0.176f, 0.241f, 0.175f, 0.238f, 0.171f, 0.233f, 0.167f, 0.234f, 0.167f, 0.232f, 0.169f, 0.233f, 0.160f, 0.232f, 0.163f, 0.224f, 0.157f, 0.227f, 0.157f, 0.219f, 0.152f, 0.216f, 0.150f, 0.215f, 0.154f, 0.214f, 0.149f, 0.210f, 0.141f, 0.212f, 0.145f, 0.207f, 0.140f, 0.207f, 0.140f, 0.203f, 0.137f, 0.201f, 0.137f, 0.205f, 0.135f, 0.202f, 0.136f, 0.198f, 0.130f, 0.195f, 0.133f, 0.197f, 0.127f, 0.194f, 0.128f, 0.192f, 0.126f, 0.193f, 0.126f, 0.191f, 0.124f, 0.189f, 0.122f, 0.187f, 0.121f, 0.187f, 0.118f, 0.182f, 0.116f, 0.183f, 0.116f, 0.181f, 0.110f, 0.176f, 0.112f, 0.177f, 0.109f, 0.175f, 0.109f, 0.173f, 0.111f, 0.172f, 0.104f, 0.174f, 0.104f, 0.174f, 0.102f, 0.167f, 0.098f, 0.168f, 0.096f, 0.168f, 0.101f, 0.165f, 0.094f, 0.164f, 0.094f, 0.160f, 0.092f, 0.163f, 0.091f, 0.158f, 0.093f, 0.157f, 0.093f, 0.156f, 0.088f, 0.157f, 0.156f, 0.088f, 0.155f, 0.086f, 0.155f, 0.086f, 0.149f, 0.084f, 0.153f, 0.079f, 0.147f, 0.081f, 0.148f, 0.085f, 0.146f
		0.283f, -0.392f, 0.276f, -0.435f, 0.304f, -0.375f, 0.304f, -0.391f, 0.321f, -0.395f, 0.264f, -0.369f, 0.323f, -0.404f, 0.300f, -0.366f, 0.305f, -0.396f, 0.288f, -0.359f, 0.313f, -0.385f, 0.324f, -0.353f, 0.326f, -0.356f, 0.335f, -0.347f, 0.360f, -0.350f, 0.350f, -0.338f, 0.361f, -0.310f, 0.405f, -0.294f, 0.428f, -0.256f, 0.403f, -0.269f, 0.449f, -0.263f, 0.477f, -0.190f, 0.551f, -0.151f, 0.579f, -0.111f, 0.602f, -0.069f, 0.672f, 0.032f, 0.737f, 0.106f, 0.816f, 0.170f, 0.889f, 0.275f, 1.019f, 0.374f, 1.106f, 0.499f, 1.275f, 0.662f, 1.414f, 0.830f, 1.660f, 1.058f, 1.846f, 1.280f, 2.103f, 1.534f, 2.371f, 1.842f, 2.706f, 2.169f, 3.064f, 2.569f, 3.430f, 2.975f, 3.917f, 3.431f, 4.397f, 3.899f, 4.909f, 4.569f, 5.495f, 5.068f, 6.517f, 5.833f, 6.819f, 6.505f, 7.611f, 7.341f, 8.475f, 8.213f, 9.395f, 9.229f, 9.906f, 11.057f, 10.927f, 12.264f, 12.201f, 13.494f, 13.523f, 14.872f, 14.864f, 16.267f, 16.515f, 17.939f, 18.112f, 19.624f, 19.671f, 21.397f, 21.694f, 23.200f, 23.716f, 25.526f, 25.367f, 27.560f, 28.130f, 29.748f, 30.384f, 32.421f, 32.789f, 35.014f, 36.103f, 38.333f, 39.354f, 42.073f, 42.961f, 46.000f, 47.131f, 50.039f, 51.218f, 54.572f, 56.092f, 59.495f, 61.494f, 64.886f, 67.166f, 70.722f, 72.743f, 76.589f, 78.770f, 81.972f, 83.961f, 87.748f, 88.531f, 92.401f, 93.365f, 96.185f, 98.164f, 100.194f, 100.467f, 101.816f, 101.016f, 101.170f, 100.744f, 100.236f, 98.030f, 98.349f, 95.131f, 94.356f, 90.454f, 89.305f, 85.523f, 83.819f, 80.048f, 77.699f, 73.367f, 71.316f, 68.127f, 65.983f, 62.236f, 60.143f, 56.832f, 54.714f, 52.669f, 51.158f, 48.452f, 47.705f, 45.046f, 43.255f, 42.955f, 40.489f, 40.038f, 38.352f, 37.795f, 36.760f, 36.777f, 35.161f, 35.382f, 34.176f, 33.937f, 33.072f, 33.130f, 32.063f, 32.541f, 31.444f, 31.587f, 30.578f, 30.767f, 29.824f, 29.912f, 28.381f, 28.745f, 26.987f, 27.242f, 25.997f, 25.985f, 24.787f, 24.860f, 23.361f, 23.605f, 22.445f, 22.204f, 21.127f, 21.130f, 19.895f, 20.119f, 18.753f, 18.857f, 17.805f, 17.923f, 16.865f, 17.112f, 15.882f, 16.269f, 15.237f, 15.528f, 14.690f, 15.108f, 14.262f, 14.599f, 13.693f, 14.197f, 13.366f, 13.900f, 13.208f, 13.641f, 12.942f, 13.526f, 12.785f, 13.476f, 12.721f, 13.270f, 12.746f, 13.165f, 12.560f, 13.270f, 12.370f, 13.118f, 12.413f, 12.951f, 12.386f, 12.920f, 12.272f, 12.964f, 12.283f, 12.910f, 12.245f, 12.803f, 12.241f, 12.878f, 12.334f, 13.053f, 12.486f, 12.499f, 13.355f, 12.704f, 13.497f, 13.066f, 13.701f, 13.340f, 14.147f, 13.728f, 14.661f, 14.111f, 15.123f, 14.715f, 15.640f, 15.344f, 16.161f, 15.933f, 16.829f, 16.474f, 17.669f, 17.208f, 18.439f, 18.087f, 19.184f, 19.020f, 19.955f, 19.857f, 20.933f, 20.630f, 21.894f, 21.606f, 22.778f, 22.618f, 23.675f, 23.604f, 24.723f, 24.540f, 25.653f, 25.716f, 26.841f, 26.683f, 28.140f, 27.753f, 29.277f, 29.096f, 30.174f, 30.457f, 31.341f, 31.497f, 32.751f, 32.558f, 34.079f, 33.868f, 35.138f, 35.128f, 36.315f, 36.419f, 37.505f, 37.862f, 38.840f, 38.940f, 40.455f, 40.036f, 41.800f, 41.141f, 42.904f, 43.041f, 43.960f, 44.101f, 45.226f, 45.368f, 46.533f, 46.473f, 47.663f, 47.543f, 49.107f, 48.557f, 50.126f, 50.083f, 51.118f, 51.085f, 52.080f, 52.144f, 52.980f, 53.077f, 54.103f, 54.473f, 54.682f, 55.441f, 55.316f, 56.604f, 56.047f, 57.239f, 56.691f, 57.750f, 57.408f, 58.475f, 58.182f, 58.899f, 58.779f, 59.429f, 59.081f, 59.840f, 59.549f, 60.431f, 59.550f, 60.988f, 60.003f, 61.100f, 60.375f, 61.220f, 60.878f, 61.116f, 61.403f, 61.670f, 61.292f, 61.937f, 61.512f, 62.223f, 61.529f, 62.771f, 61.543f, 62.918f, 61.850f, 62.901f, 62.420f, 63.122f, 62.930f, 63.147f, 62.457f, 62.851f, 63.456f, 63.198f, 63.341f, 63.756f, 63.070f, 63.992f, 63.239f, 64.410f, 63.537f, 64.381f, 63.597f, 64.557f, 64.146f, 64.245f, 63.996f, 64.332f, 64.136f, 64.093f, 64.006f, 64.170f, 63.918f, 64.808f, 63.866f, 64.683f, 63.645f, 64.954f, 63.511f, 64.868f, 63.703f, 64.801f, 63.529f, 64.802f, 64.234f, 64.766f, 63.922f, 64.503f, 64.759f, 64.688f, 64.383f, 64.766f, 64.943f, 64.643f, 64.863f, 64.865f, 64.611f, 64.861f, 64.958f, 64.901f, 64.688f, 65.015f, 65.214f, 64.510f, 64.896f, 64.363f, 64.977f, 64.302f, 65.309f, 64.185f, 64.773f, 64.002f, 65.304f, 63.962f, 64.410f, 63.785f, 64.302f, 63.822f, 64.010f, 63.761f, 63.496f, 63.459f, 63.469f, 63.651f, 63.377f, 63.527f, 63.021f, 63.064f, 62.944f, 62.939f, 62.604f, 62.200f, 62.478f, 62.384f, 62.738f, 62.086f, 62.758f, 61.676f, 62.651f, 61.576f, 62.731f, 61.488f, 62.688f, 60.978f, 62.248f, 60.476f, 61.594f, 60.120f, 61.177f, 59.664f, 60.695f, 59.404f, 59.996f, 59.105f, 59.844f, 58.756f, 59.415f, 58.676f, 58.948f, 58.078f, 58.345f, 57.137f, 57.674f, 56.794f, 57.251f, 56.402f, 56.563f, 55.827f, 55.909f, 55.616f, 55.678f, 55.801f, 55.472f, 55.586f, 55.249f, 55.186f, 54.899f, 55.383f, 53.966f, 54.958f, 53.780f, 54.480f, 53.619f, 54.280f, 53.290f, 53.985f, 53.008f, 53.400f, 52.348f, 53.346f, 52.065f, 52.931f, 51.712f, 52.491f, 51.518f, 52.216f, 51.150f, 51.714f, 50.887f, 51.347f, 50.680f, 51.096f, 50.294f, 51.042f, 49.849f, 50.777f, 49.752f, 50.301f, 49.570f, 50.200f, 49.182f, 50.120f, 49.013f, 50.136f, 48.935f, 49.966f, 48.739f, 49.963f, 48.791f, 49.677f, 48.595f, 49.419f, 48.402f, 49.297f, 48.092f, 49.273f, 47.880f, 49.023f, 47.573f, 48.621f, 47.461f, 48.260f, 47.114f, 47.813f, 46.550f, 47.603f, 46.149f, 46.934f, 45.523f, 46.160f, 45.576f, 45.702f, 44.928f, 45.273f, 44.415f, 44.689f, 43.886f, 43.991f, 43.496f, 43.247f, 42.788f, 42.871f, 42.245f, 42.347f, 41.825f, 41.651f, 41.310f, 41.117f, 40.771f, 40.785f, 40.067f, 40.345f, 39.576f, 39.101f, 39.715f, 38.490f, 39.220f, 37.994f, 38.470f, 37.603f, 37.917f, 37.145f, 37.506f, 36.597f, 36.959f, 36.453f, 36.447f, 35.988f, 36.061f, 35.527f, 35.648f, 35.083f, 35.059f, 34.622f, 34.772f, 33.958f, 34.405f, 33.388f, 33.907f, 32.950f, 33.339f, 32.358f, 32.926f, 31.819f, 32.399f, 31.562f, 31.784f, 30.996f, 31.289f, 30.485f, 30.610f, 30.005f, 29.835f, 29.488f, 29.583f, 28.998f, 29.064f, 28.441f, 28.509f, 27.736f, 28.104f, 27.143f, 27.580f, 26.550f, 27.049f, 26.008f, 26.469f, 25.387f, 25.793f, 25.019f, 25.076f, 24.539f, 24.596f, 23.906f, 23.906f, 23.441f, 23.444f, 22.831f, 23.066f, 22.358f, 22.529f, 21.766f, 22.149f, 21.157f, 21.625f, 20.828f, 21.012f, 20.294f, 20.512f, 19.789f, 19.936f, 19.351f, 19.435f, 18.736f, 18.894f, 18.211f, 18.468f, 17.569f, 17.352f, 17.552f, 16.880f, 17.074f, 16.542f, 16.555f, 15.951f, 16.175f, 15.448f, 15.695f, 14.870f, 15.361f, 14.368f, 14.863f, 13.988f, 14.469f, 13.727f, 13.973f, 13.405f, 13.497f, 12.947f, 13.262f, 12.536f, 12.955f, 12.094f, 12.607f, 11.767f, 12.237f, 11.445f, 11.925f, 11.233f, 11.537f, 10.932f, 11.224f, 10.601f, 10.883f, 10.180f, 10.641f, 9.867f, 10.334f, 9.525f, 10.092f, 9.351f, 9.739f, 9.131f, 9.439f, 8.873f, 9.234f, 8.546f, 9.072f, 8.239f, 8.827f, 8.041f, 8.560f, 7.826f, 8.260f, 7.652f, 8.376f, 7.392f, 7.838f, 7.178f, 7.690f, 6.844f, 7.437f, 6.731f, 7.211f, 6.480f, 6.938f, 6.343f, 6.769f, 6.101f, 6.564f, 5.845f, 6.428f, 5.619f, 6.258f, 5.475f, 6.025f, 5.337f, 5.807f, 5.179f, 5.642f, 4.967f, 5.570f, 4.756f, 4.729f, 5.257f, 4.537f, 5.086f, 4.363f, 4.927f, 4.171f, 4.767f, 4.068f, 4.592f, 3.900f, 4.422f, 3.786f, 4.294f, 3.567f, 4.148f, 3.406f, 3.983f, 3.285f, 3.866f, 3.172f, 3.720f, 3.023f, 3.631f, 2.919f, 3.540f, 2.777f, 3.424f, 2.710f, 3.275f, 2.633f, 3.209f, 2.503f, 3.115f, 2.376f, 2.978f, 2.318f, 2.895f, 2.229f, 2.821f, 2.141f, 2.785f, 2.090f, 2.720f, 1.982f, 2.617f, 1.926f, 2.556f, 1.868f, 2.483f, 1.767f, 2.414f, 1.705f, 2.341f, 1.686f, 2.318f, 1.573f, 2.195f, 1.501f, 2.143f, 1.414f, 2.072f, 1.404f, 2.010f, 1.350f, 1.982f, 1.329f, 1.943f, 1.263f, 1.911f, 1.223f, 1.874f, 1.163f, 1.810f, 1.122f, 1.755f, 1.108f, 1.743f, 1.023f, 1.678f, 1.010f, 1.639f, 0.923f, 1.580f, 0.931f, 1.567f, 1.547f, 0.860f, 1.531f, 0.813f, 1.460f
	)
	val vetThor = listOf(vetLambThor, vetIThor)
	var lambdasCam: List<Float> = listOf()
	var pixelInicialCam = 0f
	var pixelFinalCam = 0f
	var calibradoCam = false
	var lambdasArqv: List<Float> = listOf()
	var pixelInicialArqv = 0f
	var pixelFinalArqv = 0f
	var calibradoArqv = false
	var orientacaoSpecCam = 0
	var orientacaoSpecArqv = 0
	var posicaoCam = "10"
	var posicaoArqv = "10"
	var corretorCam: List<Float> = listOf()
	var corretorArqv: List<Float> = listOf()
	var reversoCam = false
	var reversoArqv = false


	Column(
		modifier = Modifier
			.fillMaxSize()
			.verticalScroll(ScrollState(0)),
		horizontalAlignment = Alignment.CenterHorizontally
	) {

		JanelaCamera(previewView)

		Column(
			modifier = Modifier.padding(5.dp),
			verticalArrangement = Arrangement.Bottom
		) {

			GraficoBotao(
				comeco = inicio,
				fim = final,
				xizes = abcisas,
				onClickCam = { cameraUIAction(CameraUIAction.OnCameraClickSpectre) },
				onClickTroca = { cameraUIAction(CameraUIAction.OnSwitchCameraClick) },
				onClickArqv = { cameraUIAction(CameraUIAction.OnGalleryViewClick) },
				onClickCamOk = {
					if (!calculando) {
						calculando = true
						avisos = "calculando"
						val sensibVal = try {
							inputSensibilidade.toInt()
						} catch (e: NumberFormatException) {
							10
						}
						val espessuraVal = try {
							inputEspessura.toInt()
						} catch (e: NumberFormatException) {
							50
						}
						val lamb1Val = try {
							inputLamb1.toInt()
						} catch (e: NumberFormatException) {
							lamb1Inicial.toInt()
						}
						val lamb2Val = try {
							inputLamb2.toInt()
						} catch (e: NumberFormatException) {
							lamb2Inicial.toInt()
						}
						medidas = if (calibradoCam){
							mensurarSpectre2(
								uriCamSpectre,
								corretorCam,
								posicaoCam.toInt(),
								pixelInicialCam.toInt(),
								pixelFinalCam.toInt(),
								orientacaoSpecCam,
								reversoCam,
								espessuraVal,
								context = context
							)
						} else {
							mensurarSpectre(
								uriCamSpectre,
								corretor,
								sensibVal,
								espessuraVal,
								orientacaoSpec,
								null,
								context = context
							)
						}
						//Log.i("QQQQQQQQQQQQQQQQQQQ", medidas[1].toString())
						calculando = false
						avisos = "ok"
						points = medidas[1]
						return@GraficoBotao medidas[1]
					}
					return@GraficoBotao listOf(2f, 2f)

				},
				onClickArqvOk = {
					if (!calculando) {
						calculando = true
						avisos = "calculando"
						val sensibVal = try {
							inputSensibilidade.toInt()
						} catch (e: NumberFormatException) {
							10
						}
						val espessuraVal = try {
							inputEspessura.toInt()
						} catch (e: NumberFormatException) {
							50
						}
						val lamb1Val = try {
							inputLamb1.toInt()
						} catch (e: NumberFormatException) {
							lamb1Inicial.toInt()
						}
						val lamb2Val = try {
							inputLamb2.toInt()
						} catch (e: NumberFormatException) {
							lamb2Inicial.toInt()
						}
						medidas = if (calibradoArqv) {
							mensurarSpectre2(
								uriArqvSpectre,
								corretorArqv,
								posicaoArqv.toInt(),
								pixelInicialArqv.toInt(),
								pixelFinalArqv.toInt(),
								orientacaoSpecArqv,
								reversoArqv,
								espessuraVal,
								context = context
							)
						}else{
							mensurarSpectre(
								uriArqvSpectre,
								corretor,
								sensibVal,
								espessuraVal,
								orientacaoSpec,
								null,
								context
							)
						}
						//Log.i("QQQQQQQQQQQQQQQQQQQ", medidas[1].toString())
						calculando = false
						avisos = "ok"
						points = medidas[1]
						return@GraficoBotao medidas[1]
					}
					return@GraficoBotao listOf(2f, 2f)
				},
				onClickLuz = {
					torchOn = !torchOn
					camera?.cameraControl?.enableTorch(torchOn)
				},
				verificador = torchOn
			)

			BarraFBG(
			)

			var tempCentro = 0
			var tempSpan = 0
			var tempIni = 0
			var tempFin = 0
			BarraSpan(
				"123",
				"234",
				"345",
				"456",
				onClick1 = { /* CENTRO */ },
				onClick2 = { /* SPAN */ },
				onClick3 = { /* INICIO */},
				onClick4 = { /* FINAL */},
				onClick5 = {strC: String, strS: String -> // CENTRO
					tempCentro = strC.toInt()
					tempSpan = strS.toInt()
					inicio = tempCentro - tempSpan/2
					final = tempCentro + tempSpan/2 + 1
				},
				onClick6 = {str1: String, str2: String -> // INICIO E FIM
					tempIni = str1.toInt()
					tempFin = str2.toInt()
					if (tempIni < tempFin) {
						inicio = tempIni
						final = tempFin + 1
					} else {
						inicio = tempFin
						final = tempIni + 1
					}
				},
				onClick7 = { // RESET
					inicio = defaultInicio ?: 321
					final = null
				},
			)

			// BOTAO DE SALVAR
			Row (
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.Center,
				verticalAlignment = Alignment.CenterVertically
			){
				TextButton(
					onClick = { },
					color = Color.Yellow,
					texto = "SAVE DATA",
					textColor = Color.Red
				)
			}

			// BOTOES PARA CALIBRAGEM
			Spacer(modifier = Modifier.height(8.dp))
			ButtonRowN5Cam(
				cameraUIAction = cameraUIAction,
				onClickOk = {
					// calibragem
					if (uriCamBranco != null && uriCamLamb1 != null && uriCamLamb2 != null) {
						//RETORNO: vetLambdas; corretor; pixelInicial; pixelFinal; posicao; orientacao; reverso;
						val temp = Calibrar(
							uriBranco = uriCamBranco,
							uriLamb1 = uriCamLamb1,
							uriLamb2 = uriCamLamb2,
							lamb1 = inputLamb1.toFloat(),
							lamb2 = inputLamb2.toFloat(),
							vetThor = vetThor,
							sensibilidade = inputSensibilidade.toInt(),
							Espessura = inputEspessura.toInt(),
							orientacaoPre = orientacaoSpec,
							posicaoUser = null,
							context = context
						)
						lambdasCam = temp[0] as List<Float>
						corretorCam = temp[1] as List<Float>
						pixelInicialCam = temp[2] as Float
						pixelFinalCam = temp[3] as Float
						posicaoCam = temp[4].toString()
						orientacaoSpecCam = temp[5] as Int
						reversoCam = temp[6] as Boolean
						calibradoCam = temp[7] as Boolean
					}
					return@ButtonRowN5Cam true
				},
				cor = Color.Magenta
			)

			ButtonRowN5Arqv(
				cameraUIAction = cameraUIAction,
				onClickOk = {
					if (uriArqvBranco != null && uriArqvLamb1 != null && uriArqvLamb2 != null) {
						val temp = Calibrar(
							uriBranco = uriArqvBranco,
							uriLamb1 = uriArqvLamb1,
							uriLamb2 = uriArqvLamb2,
							lamb1 = inputLamb1.toFloat(),
							lamb2 = inputLamb2.toFloat(),
							vetThor = vetThor,
							sensibilidade = inputSensibilidade.toInt(),
							Espessura = inputEspessura.toInt(),
							orientacaoPre = orientacaoSpec,
							posicaoUser = null,
							context = context
						)
						lambdasArqv = temp[0] as List<Float>
						corretorArqv = temp[1] as List<Float>
						pixelInicialArqv = temp[2] as Float
						pixelFinalArqv = temp[3] as Float
						posicaoArqv = temp[4].toString()
						orientacaoSpecArqv = temp[5] as Int
						reversoArqv = temp[6] as Boolean
						calibradoArqv = temp[7] as Boolean
					}
					return@ButtonRowN5Arqv true
				},
				cor = Color.Blue
			)


			Text(text = "general calibration options:",
				Modifier
					.background(color = Color.LightGray)
					.fillMaxWidth())

			// LAMBDAS
			LambdasBar(
				inputLamb1 = lamb1Inicial,
				inputLamb2 = lamb2Inicial,
				onClick1 = {inputLamb1 = it},
				onClick2 = {inputLamb2 = it}
			)

			// SENSIBILIDADE
			Sensibilidade(
				"80",
				"10",
				"500",
				{ inputSensibilidade = it },
				{ inputEspessura = it },
				{ inputPosicao = it }
			)

			// ORIENTAÇÃO DO ESPECTRO
			BarraOrientacao(
				onClick1 = { orientacaoSpec = 1 },
				onClick2 = { orientacaoSpec = 2 },
				onClick3 = { orientacaoSpec = 3 }
			)

		}

		BarraInfo()

	}
}




