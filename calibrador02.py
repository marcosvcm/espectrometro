import cv2
import matplotlib.pyplot as plt
import math
import csv


streamUrl = "http://192.168.0.168:81/stream"
pathDadosThor = "dados experimentais thorlabs/led branco-280524.csv"
calculando = False
vezes = 50
sensibilidade = 50
espessura = 10
# calibracao
lamb1 = 532
lamb2 = 650
posLamb1 = 1
posLamb2 = 10
passo = 1
vetThor = []
lambInicial = 400
posicao = 590
# RETORNAR
pixelIni = 1
pixelFin = 3
vetCorretor = []
vetLambdas = []


def modulo(n):
	if n < 0: return -n
	return n


def calcSpectre(red, green, blue):
	return (red + green/2 + blue)/3


def normalizarVet(vet):
	max = 0
	for i in vet:
		if i > max: max = i
	res = [x/max for x in vet]
	return res


# ACHAR POSLAMBDA1 E POSLAMBDA2
cap = cv2.VideoCapture(streamUrl)

_ = input("use a fonte de lambda1.\nPress enter to continue.")

for x in range(vezes):
	ret, frame = cap.read()
	if not ret:
		print("Error reading frame from stream")
		cap.release()
		cv2.destroyAllWindows()
		cap = cv2.VideoCapture(streamUrl)
	else:
		# Process RGB values
		esq = 0
		dir = 0
		for pixel in frame[posicao]:
			r, g, b = pixel
			if calcSpectre(r, g, b) > sensibilidade: break
			esq += 1
		for pixel in frame[posicao][esq+1:]:
			r, g, b = pixel
			if calcSpectre(r, g, b) < sensibilidade: break
			dir += 1
		posLamb1 += (esq + dir) / 2
posLamb1 = int(posLamb1/vezes)
print("posLamb1 = {}".format(posLamb1))

_ = input("use a fonte de lambda2\nPress enter to continue.")

for x in range(vezes):
	ret, frame = cap.read()
	if not ret:
		print("Error reading frame from stream")
		cap.release()
		cv2.destroyAllWindows()
		cap = cv2.VideoCapture(streamUrl)
	else:
		# Process RGB values
		esq = 0
		dir = 0
		for pixel in frame[posicao]:
			r, g, b = pixel
			if calcSpectre(r, g, b) > sensibilidade: break
			esq += 1
		for pixel in frame[posicao][esq+1:]:
			r, g, b = pixel
			if calcSpectre(r, g, b) < sensibilidade: break
			dir += 1
		posLamb2 += (esq + dir) / 2
posLamb2 = int(posLamb2/vezes)
print("posLamb2 = {}".format(posLamb2))

cap.release()
cv2.destroyAllWindows()


# COMPUTAR PASSO
passo = modulo((lamb2 - lamb1) / (posLamb2 - posLamb1))
print("resolução = {} nm/pixel".format(passo))


# DETERMINAR PIXEL INICIAL E FINAL
if posLamb1 < posLamb2:
	pixelIni = math.floor(posLamb1 - ((lamb1-400)/passo))
	pixelFin = math.ceil(posLamb1 + ((700-lamb1)/passo))
else:
	pixelIni = math.ceil(posLamb1 + ((lamb1-400)/passo))
	pixelFin = math.floor(posLamb1 - ((700-lamb1)/passo))
print("pixelIni = {}".format(pixelIni))
print("pixelFin = {}".format(pixelFin))


# CRIAR VETLAMBDAS
lambdaIni = lamb1-modulo(posLamb1-pixelIni)*passo
print("lambdaIni = {}".format(lambdaIni))
vetLambdas = [lambdaIni + x*passo for x in range(modulo(pixelFin-pixelIni))]
print("tam vetLambdas = {}".format(len(vetLambdas)))


# VETINTENSIDADES
vetIntensidades = [0 for x in range(len(vetLambdas))]

cap = cv2.VideoCapture(streamUrl)
_ = input("use a fonte LED branco.\nPress enter to continue.")

for x in range(vezes):
	intensidadesTemp = []
	ret, frame = cap.read()
	if not ret:
		print("Error reading frame from stream")
		cap.release()
		cv2.destroyAllWindows()
		cap = cv2.VideoCapture(streamUrl)
	else:
		# Process RGB values
		comeco = pixelIni if pixelIni < pixelFin else pixelFin
		fim = pixelFin if pixelIni < pixelFin else pixelIni
		for pixel in frame[posicao][comeco:fim]:
			r, g, b = pixel
			intensidadesTemp.append(calcSpectre(r, g, b))
		for j in range(len(intensidadesTemp)):
			vetIntensidades[j] += intensidadesTemp[j]
		for i1 in range(1, espessura // 2):
			linha = frame[posicao - i1][comeco:fim]
			for i2 in range(len(vetIntensidades)):
				r, g, b = linha[i2]
				vetIntensidades[i2] += calcSpectre(r, g, b)
			linha = frame[posicao + i1][comeco:fim]
			for i2 in range(len(vetIntensidades)):
				r, g, b = linha[i2]
				vetIntensidades[i2] += calcSpectre(r, g, b)

cap.release()
cv2.destroyAllWindows()

for i in range(len(vetIntensidades)):
	vetIntensidades[i] = vetIntensidades[i] / (espessura * vezes)

if posLamb1 > posLamb2:
	vetTemp = vetIntensidades
	for i in range(len(vetIntensidades)):
		vetIntensidades[i] = vetTemp[-i]

print("tam vetIntensidades = {}".format(len(vetIntensidades)))


# VETINTENSIDADESTHOR
# importando dados do espectrometro throlabs
with open(pathDadosThor) as dadoscsv:
	dadoscsv_reader = csv.reader(dadoscsv, delimiter=',')
	dados_thor = [[float(x[0]), float(x[1])] for x in dadoscsv_reader]
print("tam thor = {}".format(len(dados_thor)))
print("thor[3] = {}".format((dados_thor[3])))


#VETCORRETOR
vetCorretor = []

j = 0
for i in range(len(vetIntensidades)):
	while dados_thor[j][0] < vetLambdas[i]:
		j += 1
	if dados_thor[j][0] > vetLambdas[i]: 
		vetCorretor.append(((dados_thor[j][1] + dados_thor[j-1][1])/2)/vetIntensidades[i])
	else: vetCorretor.append(dados_thor[j][1]/vetIntensidades[i])

print("tam vetCorretor = {}".format(len(vetCorretor)))
print("vetCorretor = {}".format(vetCorretor))

print("vetLambdas = {}".format(vetLambdas))

plt.plot(vetLambdas, vetCorretor)
plt.grid()
plt.show()

