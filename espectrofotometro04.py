import cv2
import matplotlib.pyplot as plt
import threading
import time


stream_url = "http://192.168.0.168:81/stream"
calculando = False
calculandoBlank = False
calculandoVett = False
escrevendo = False
escrevendoBlank = False
escrevendoVett = False
contagem = 0
medMov = []
medMovVett = []
spectreBlank = []
spectreVett = []
spectre = []
normalizador = 0
vetLambdas =  [
	399.84000000000003, 400.23333333333335, 400.6266666666667, 401.02000000000004, 401.41333333333336, 401.8066666666667, 402.20000000000005, 402.59333333333336, 402.9866666666667, 403.38000000000005, 403.77333333333337, 404.1666666666667, 404.56000000000006, 404.9533333333334, 405.3466666666667, 405.74, 406.1333333333334, 406.5266666666667, 406.92, 407.3133333333334, 407.7066666666667, 408.1, 408.49333333333334, 408.8866666666667, 409.28000000000003, 409.67333333333335, 410.0666666666667, 410.46000000000004, 410.85333333333335, 411.24666666666667, 411.64000000000004, 412.03333333333336, 412.4266666666667, 412.82000000000005, 413.21333333333337, 413.6066666666667, 414.00000000000006, 414.3933333333334, 414.7866666666667, 415.18, 415.5733333333334, 415.9666666666667, 416.36, 416.7533333333334, 417.1466666666667, 417.54, 417.9333333333334, 418.3266666666667, 418.72, 419.11333333333334, 419.5066666666667, 419.90000000000003, 420.29333333333335, 420.6866666666667, 421.08000000000004, 421.47333333333336, 421.8666666666667, 422.26000000000005, 422.65333333333336, 423.0466666666667, 423.44000000000005, 423.83333333333337, 424.2266666666667, 424.62, 425.0133333333334, 425.4066666666667, 425.8, 426.1933333333334, 426.5866666666667, 426.98, 427.37333333333333, 427.7666666666667, 428.16, 428.55333333333334, 428.9466666666667, 429.34000000000003, 429.73333333333335, 430.1266666666667, 430.52000000000004, 430.91333333333336, 431.3066666666667, 431.70000000000005, 432.09333333333336, 432.4866666666667, 432.88000000000005, 433.27333333333337, 433.6666666666667, 434.06000000000006, 434.4533333333334, 434.8466666666667, 435.24, 435.6333333333334, 436.0266666666667, 436.42, 436.8133333333334, 437.2066666666667, 437.6, 437.99333333333334, 438.3866666666667, 438.78000000000003, 439.17333333333335, 439.5666666666667, 439.96000000000004, 440.35333333333335, 440.74666666666667, 441.14000000000004, 441.53333333333336, 441.9266666666667, 442.32000000000005, 442.71333333333337, 443.1066666666667, 443.5, 443.8933333333334, 444.2866666666667, 444.68, 445.0733333333334, 445.4666666666667, 445.86, 446.2533333333334, 446.6466666666667, 447.04, 447.4333333333334, 447.8266666666667, 448.22, 448.61333333333334, 449.0066666666667, 449.40000000000003, 449.79333333333335, 450.1866666666667, 450.58000000000004, 450.97333333333336, 451.3666666666667, 451.76000000000005, 452.15333333333336, 452.5466666666667, 452.94000000000005, 453.33333333333337, 453.7266666666667, 454.12, 454.5133333333334, 454.9066666666667, 455.3, 455.6933333333334, 456.0866666666667, 456.48, 456.87333333333333, 457.2666666666667, 457.66, 458.05333333333334, 458.4466666666667, 458.84000000000003, 459.23333333333335, 459.62666666666667, 460.02000000000004, 460.41333333333336, 460.8066666666667, 461.20000000000005, 461.59333333333336, 461.9866666666667, 462.38000000000005, 462.77333333333337, 463.1666666666667, 463.56000000000006, 463.9533333333334, 464.3466666666667, 464.74, 465.1333333333334, 465.5266666666667, 465.92, 466.3133333333334, 466.7066666666667, 467.1, 467.49333333333334, 467.8866666666667, 468.28000000000003, 468.67333333333335, 469.0666666666667, 469.46000000000004, 469.85333333333335, 470.24666666666667, 470.64000000000004, 471.03333333333336, 471.4266666666667, 471.82000000000005, 472.21333333333337, 472.6066666666667, 473.0, 473.3933333333334, 473.7866666666667, 474.18, 474.5733333333334, 474.9666666666667, 475.36, 475.75333333333333, 476.1466666666667, 476.54, 476.9333333333334, 477.3266666666667, 477.72, 478.11333333333334, 478.50666666666666, 478.90000000000003, 479.29333333333335, 479.6866666666667, 480.08000000000004, 480.47333333333336, 480.8666666666667, 481.26000000000005, 481.65333333333336, 482.0466666666667, 482.44000000000005, 482.83333333333337, 483.2266666666667, 483.62, 484.0133333333334, 484.4066666666667, 484.8, 485.1933333333334, 485.5866666666667, 485.98, 486.37333333333333, 486.7666666666667, 487.16, 487.55333333333334, 487.9466666666667, 488.34000000000003, 488.73333333333335, 489.12666666666667, 489.52000000000004, 489.91333333333336, 490.30666666666673, 490.70000000000005, 491.09333333333336, 491.4866666666667, 491.88, 492.27333333333337, 492.6666666666667, 493.06000000000006, 493.4533333333334, 493.8466666666667, 494.24, 494.6333333333333, 495.0266666666667, 495.42, 495.8133333333334, 496.2066666666667, 496.6, 496.99333333333334, 497.3866666666667, 497.78000000000003, 498.17333333333335, 498.5666666666667, 498.96000000000004, 499.35333333333335, 499.74666666666667, 500.14000000000004, 500.53333333333336, 500.9266666666667, 501.32000000000005, 501.71333333333337, 502.1066666666667, 502.5, 502.8933333333334, 503.2866666666667, 503.68, 504.0733333333334, 504.4666666666667, 504.86, 505.25333333333333, 505.6466666666667, 506.04, 506.4333333333334, 506.8266666666667, 507.22, 507.61333333333334, 508.00666666666666, 508.40000000000003, 508.79333333333335, 509.1866666666667, 509.58000000000004, 509.97333333333336, 510.3666666666667, 510.76, 511.15333333333336, 511.5466666666667, 511.94000000000005, 512.3333333333334, 512.7266666666667, 513.12, 513.5133333333333, 513.9066666666668, 514.3000000000001, 514.6933333333334, 515.0866666666667, 515.48, 515.8733333333333, 516.2666666666667, 516.6600000000001, 517.0533333333334, 517.4466666666667, 517.84, 518.2333333333333, 518.6266666666667, 519.02, 519.4133333333334, 519.8066666666667, 520.2, 520.5933333333334, 520.9866666666667, 521.38, 521.7733333333333, 522.1666666666667, 522.5600000000001, 522.9533333333334, 523.3466666666667, 523.74, 524.1333333333333, 524.5266666666666, 524.9200000000001, 525.3133333333334, 525.7066666666667, 526.1, 526.4933333333333, 526.8866666666667, 527.28, 527.6733333333334, 528.0666666666667, 528.46, 528.8533333333334, 529.2466666666667, 529.64, 530.0333333333333, 530.4266666666667, 530.82, 531.2133333333334, 531.6066666666667, 532.0, 532.3933333333333, 532.7866666666666, 533.1800000000001, 533.5733333333334, 533.9666666666667, 534.36, 534.7533333333333, 535.1466666666668, 535.54, 535.9333333333334, 536.3266666666667, 536.72, 537.1133333333333, 537.5066666666667, 537.9000000000001, 538.2933333333333, 538.6866666666667, 539.08, 539.4733333333334, 539.8666666666667, 540.26, 540.6533333333334, 541.0466666666666, 541.44, 541.8333333333334, 542.2266666666667, 542.62, 543.0133333333333, 543.4066666666668, 543.8, 544.1933333333334, 544.5866666666667, 544.98, 545.3733333333333, 545.7666666666667, 546.1600000000001, 546.5533333333334, 546.9466666666667, 547.34, 547.7333333333333, 548.1266666666667, 548.52, 548.9133333333334, 549.3066666666667, 549.7, 550.0933333333334, 550.4866666666667, 550.88, 551.2733333333333, 551.6666666666667, 552.0600000000001, 552.4533333333334, 552.8466666666667, 553.24, 553.6333333333333, 554.0266666666666, 554.4200000000001, 554.8133333333334, 555.2066666666667, 555.6, 555.9933333333333, 556.3866666666667, 556.78, 557.1733333333334, 557.5666666666667, 557.96, 558.3533333333334, 558.7466666666667, 559.14, 559.5333333333333, 559.9266666666667, 560.32, 560.7133333333334, 561.1066666666667, 561.5, 561.8933333333333, 562.2866666666666, 562.6800000000001, 563.0733333333334, 563.4666666666667, 563.86, 564.2533333333333, 564.6466666666668, 565.04, 565.4333333333334, 565.8266666666667, 566.22, 566.6133333333333, 567.0066666666667, 567.4000000000001, 567.7933333333333, 568.1866666666667, 568.58, 568.9733333333334, 569.3666666666667, 569.76, 570.1533333333334, 570.5466666666666, 570.94, 571.3333333333334, 571.7266666666667, 572.12, 572.5133333333333, 572.9066666666668, 573.3, 573.6933333333334, 574.0866666666667, 574.48, 574.8733333333333, 575.2666666666667, 575.6600000000001, 576.0533333333333, 576.4466666666667, 576.84, 577.2333333333333, 577.6266666666667, 578.02, 578.4133333333334, 578.8066666666667, 579.2, 579.5933333333334, 579.9866666666667, 580.38, 580.7733333333333, 581.1666666666667, 581.5600000000001, 581.9533333333334, 582.3466666666667, 582.74, 583.1333333333333, 583.5266666666666, 583.9200000000001, 584.3133333333334, 584.7066666666667, 585.1, 585.4933333333333, 585.8866666666667, 586.28, 586.6733333333334, 587.0666666666667, 587.46, 587.8533333333334, 588.2466666666667, 588.64, 589.0333333333333, 589.4266666666667, 589.82, 590.2133333333334, 590.6066666666667, 591.0, 591.3933333333333, 591.7866666666666, 592.1800000000001, 592.5733333333334, 592.9666666666667, 593.36, 593.7533333333333, 594.1466666666666, 594.54, 594.9333333333334, 595.3266666666667, 595.72, 596.1133333333333, 596.5066666666667, 596.9000000000001, 597.2933333333333, 597.6866666666667, 598.08, 598.4733333333334, 598.8666666666667, 599.26, 599.6533333333334, 600.0466666666666, 600.44, 600.8333333333334, 601.2266666666667, 601.62, 602.0133333333333, 602.4066666666668, 602.8, 603.1933333333334, 603.5866666666667, 603.98, 604.3733333333333, 604.7666666666667, 605.1600000000001, 605.5533333333333, 605.9466666666667, 606.34, 606.7333333333333, 607.1266666666667, 607.52, 607.9133333333334, 608.3066666666667, 608.7, 609.0933333333334, 609.4866666666667, 609.88, 610.2733333333333, 610.6666666666667, 611.0600000000001, 611.4533333333334, 611.8466666666667, 612.24, 612.6333333333333, 613.0266666666666, 613.4200000000001, 613.8133333333334, 614.2066666666667, 614.6, 614.9933333333333, 615.3866666666667, 615.78, 616.1733333333334, 616.5666666666667, 616.96, 617.3533333333334, 617.7466666666667, 618.14, 618.5333333333333, 618.9266666666667, 619.32, 619.7133333333334, 620.1066666666667, 620.5, 620.8933333333333, 621.2866666666666, 621.6800000000001, 622.0733333333334, 622.4666666666667, 622.86, 623.2533333333333, 623.6466666666666, 624.04, 624.4333333333334, 624.8266666666667, 625.22, 625.6133333333333, 626.0066666666667, 626.4000000000001, 626.7933333333333, 627.1866666666667, 627.58, 627.9733333333334, 628.3666666666667, 628.76, 629.1533333333334, 629.5466666666666, 629.94, 630.3333333333334, 630.7266666666667, 631.12, 631.5133333333333, 631.9066666666668, 632.3, 632.6933333333334, 633.0866666666667, 633.48, 633.8733333333333, 634.2666666666667, 634.6600000000001, 635.0533333333333, 635.4466666666667, 635.84, 636.2333333333333, 636.6266666666667, 637.02, 637.4133333333334, 637.8066666666666, 638.2, 638.5933333333334, 638.9866666666667, 639.38, 639.7733333333333, 640.1666666666667, 640.5600000000001, 640.9533333333334, 641.3466666666667, 641.74, 642.1333333333333, 642.5266666666666, 642.9200000000001, 643.3133333333334, 643.7066666666667, 644.1, 644.4933333333333, 644.8866666666667, 645.28, 645.6733333333334, 646.0666666666667, 646.46, 646.8533333333334, 647.2466666666667, 647.64, 648.0333333333333, 648.4266666666667, 648.82, 649.2133333333334, 649.6066666666667, 650.0, 650.3933333333333, 650.7866666666666, 651.1800000000001, 651.5733333333334, 651.9666666666667, 652.36, 652.7533333333333, 653.1466666666666, 653.54, 653.9333333333334, 654.3266666666667, 654.72, 655.1133333333333, 655.5066666666667, 655.9000000000001, 656.2933333333333, 656.6866666666667, 657.08, 657.4733333333334, 657.8666666666667, 658.26, 658.6533333333334, 659.0466666666666, 659.44, 659.8333333333334, 660.2266666666667, 660.62, 661.0133333333333, 661.4066666666668, 661.8, 662.1933333333334, 662.5866666666667, 662.98, 663.3733333333333, 663.7666666666667, 664.1600000000001, 664.5533333333333, 664.9466666666667, 665.34, 665.7333333333333, 666.1266666666667, 666.52, 666.9133333333334, 667.3066666666666, 667.7, 668.0933333333334, 668.4866666666667, 668.88, 669.2733333333333, 669.6666666666667, 670.06, 670.4533333333334, 670.8466666666667, 671.24, 671.6333333333333, 672.0266666666666, 672.4200000000001, 672.8133333333333, 673.2066666666667, 673.6, 673.9933333333333, 674.3866666666667, 674.78, 675.1733333333334, 675.5666666666666, 675.96, 676.3533333333334, 676.7466666666667, 677.1400000000001, 677.5333333333333, 677.9266666666667, 678.3199999999999, 678.7133333333334, 679.1066666666667, 679.5, 679.8933333333334, 680.2866666666666, 680.6800000000001, 681.0733333333333, 681.4666666666667, 681.86, 682.2533333333333, 682.6466666666668, 683.04, 683.4333333333334, 683.8266666666667, 684.22, 684.6133333333333, 685.0066666666667, 685.4000000000001, 685.7933333333333, 686.1866666666667, 686.58, 686.9733333333334, 687.3666666666667, 687.76, 688.1533333333334, 688.5466666666666, 688.94, 689.3333333333334, 689.7266666666667, 690.12, 690.5133333333333, 690.9066666666668, 691.3, 691.6933333333334, 692.0866666666667, 692.48, 692.8733333333333, 693.2666666666667, 693.6600000000001, 694.0533333333333, 694.4466666666667, 694.84, 695.2333333333333, 695.6266666666667, 696.02, 696.4133333333334, 696.8066666666666, 697.2, 697.5933333333334, 697.9866666666667, 698.38, 698.7733333333333, 699.1666666666667, 699.56, 699.9533333333334]



def calcSpectre(red, green, blue):
	return (red + green/2 + blue)/3


def normalizarVet(vet):
	max = 0
	for i in vet:
		if i > max: max = i
	res = [x/max for x in vet]
	return res


def acharNormalizador(vet):
	res = 0
	for x in vet:
		if x > res: res = x
	return res


def streamVideo(streamUrl, espessura, vezes, pixIni, pixFin):
	global calculando
	tentarNovamente = False
	cap = cv2.VideoCapture(streamUrl)
	
	while(True):
		ret, frame = cap.read()
		if not ret:
			print("Error reading frame from stream")
			tentarNovamente = True
			break
		else:
			# Process RGB values
			if not calculando:
				calculando = True
				t1 = threading.Thread(target=processLinGlobal, args=(frame, espessura, vezes, pixIni, pixFin))
				t1.start()
				print("foooooooooooooo")

			# Display the frame
			cv2.imshow("ESP32-CAM Stream", frame)

			# Press 'q' to exit
			if cv2.waitKey(1) & 0xFF == ord('q'):
				break

	cap.release()
	cv2.destroyAllWindows()

	if tentarNovamente: streamVideo(streamUrl, espessura, vezes, pixIni, pixFin)


def streamVideoVett(streamUrl, espessura, vezes, pixIni, pixFin):
	global calculandoVett
	tentarNovamente = False
	cap = cv2.VideoCapture(streamUrl)
	
	while(True):
		ret, frame = cap.read()
		if not ret:
			print("Error reading frame from stream")
			tentarNovamente = True
			break
		else:
			# Process RGB values
			if not calculandoVett:
				calculandoVett = True
				t1 = threading.Thread(target=processLinGVett, args=(frame, espessura, vezes, pixIni, pixFin))
				t1.start()
				print("foooooooooooooo")

			# Display the frame
			cv2.imshow("ESP32-CAM Stream", frame)

			# Press 'q' to exit
			if cv2.waitKey(1) & 0xFF == ord('q'):
				break

	cap.release()
	cv2.destroyAllWindows()

	if tentarNovamente: streamVideoVett(streamUrl, espessura, vezes, pixIni, pixFin)


def streamFoto(streamUrl, espessura, vezes, pixIni, pixFin):
	global calculando
	tentarNovamente = False
	cap = cv2.VideoCapture(streamUrl)
	
	while(True):
		ret, frame = cap.read()
		if not ret:
			print("Error reading frame from stream")
			tentarNovamente = True
			break
		else:
			# Process RGB values
			if not calculando:
				calculando = True
				t1 = threading.Thread(target=processLinGlobal, args=(frame, espessura, vezes, pixIni, pixFin))
				t1.start()
				print("badabing")

			# Display the frame
			cv2.imshow("ESP32-CAM Stream", frame)

			break

	while (True):
		if cv2.waitKey(1) & 0xFF == ord('q'):
			cap.release()
			cv2.destroyAllWindows()
			break

	if tentarNovamente: streamFoto(streamUrl, espessura, vezes, pixIni, pixFin)


def streamFotoVett(streamUrl, espessura, vezes, pixIni, pixFin):
	global calculandoVett
	tentarNovamente = False
	cap = cv2.VideoCapture(streamUrl)
	
	while(True):
		ret, frame = cap.read()
		if not ret:
			print("Error reading frame from stream")
			tentarNovamente = True
			break
		else:
			# Process RGB values
			if not calculandoVett:
				calculandoVett = True
				t1 = threading.Thread(target=processLinGVett, args=(frame, espessura, vezes, pixIni, pixFin))
				t1.start()
				print("badabing")

			# Display the frame
			cv2.imshow("ESP32-CAM Stream", frame)

			break

	while (True):
		if cv2.waitKey(1) & 0xFF == ord('q'):
			cap.release()
			cv2.destroyAllWindows()
			break

	if tentarNovamente: streamFotoVett(streamUrl, espessura, vezes, pixIni, pixFin)


def streamGraf(streamUrl, espessura, vezes, pixIni, pixFin):
	global calculando
	tentarNovamente = False
	cap = cv2.VideoCapture(streamUrl)

	while(True):
		ret, frame = cap.read()
		if not ret:
			print("Error reading frame from stream")
			tentarNovamente = True
			break
		else:
			# Process RGB values
			if not calculando:
				calculando = True
				t1 = threading.Thread(target=processLinGlobal, args=(frame, espessura, vezes, pixIni, pixFin, ))
				t1.start()
				print("foooooooooooooo")

	cap.release()
	cv2.destroyAllWindows()

	if tentarNovamente: streamGraf(streamUrl, espessura, vezes, pixIni, pixFin)


def streamGrafVett(streamUrl, espessura, vezes, pixIni, pixFin):
	global calculandoVett
	tentarNovamente = False
	cap = cv2.VideoCapture(streamUrl)

	while(True):
		ret, frame = cap.read()
		if not ret:
			print("Error reading frame from stream")
			tentarNovamente = True
			break
		else:
			# Process RGB values
			if not calculandoVett:
				calculandoVett = True
				t1 = threading.Thread(target=processLinGVett, args=(frame, espessura, vezes, pixIni, pixFin, ))
				t1.start()
				print("foooooooooooooo")

	cap.release()
	cv2.destroyAllWindows()

	if tentarNovamente: streamGrafVett(streamUrl, espessura, vezes, pixIni, pixFin)


def processLin(frame):
	res = []
	height, width, _ = frame.shape
	center_row = height // 2

	linha = frame[center_row]
	for pixel in linha:
		r, g, b = pixel
		res.append(calcSpectre(int(r), int(g), int(b)))
	return normalizarVet(res)


def processLinGlobal(frame, espessura, vezes, pixIni, pixFin): 
	global escrevendo
	global calculando
	global spectre
	global contagem
	global medMov

	if contagem >= vezes: contagem = 0
	if espessura < 1: espessura = 1
	if vezes < 1: vezes = 1
	res = []
	height, width, _ = frame.shape
	posLinha = height // 2

	comeco = pixIni if pixIni < pixFin else pixFin
	fim = pixFin if pixIni < pixFin else pixIni
	linha = frame[posLinha]
	for pixel in linha[comeco:fim]:
		r, g, b = pixel
		res.append(calcSpectre(int(r), int(g), int(b)))
	for i in range(1, espessura // 2):
		n = 0
		linha = frame[posLinha - i]
		for pixel in linha[comeco:fim]:
			r, g, b = pixel
			res[n] = calcSpectre(int(r), int(g), int(b))
			n += 1
		n = 0
		linha = frame[posLinha + i]
		for pixel in linha[comeco:fim]:
			r, g, b = pixel
			res[n] = calcSpectre(int(r), int(g), int(b))
			n += 1

	try:
		medMov[contagem] = res
	except:
		medMov.append(res)

	contagem += 1

	res = medMov[0]
	for i in range(len(res)):
		try:
			for j in range(1, len(medMov)):
				res[i] += medMov[j][i] 
		except:
			break

	div = len(medMov) * espessura
	for i in range(len(res)):
		res[i] = res[i]/div

	if not escrevendo:
		escrevendo = True
		spectre = normalizarVet(res)
		escrevendo = False
	calculando = False


def processLinGVett(frame, espessura, vezes, pixIni, pixFin):
	global escrevendoVett
	global calculandoVett
	global spectreVett
	global spectreBlank
	global contagem
	global medMovVett
	global normalizador

	if contagem >= vezes:
		contagem = 0
		print("000000000000000000000000000")
	if espessura < 1: espessura = 1
	if vezes < 1: vezes = 1
	res = []
	height, width, _ = frame.shape
	posLinha = height // 2

	comeco = pixIni if pixIni < pixFin else pixFin
	fim = pixFin if pixIni < pixFin else pixIni
	linha = frame[posLinha]
	for pixel in linha[comeco:fim]:
		r, g, b = pixel
		res.append(calcSpectre(int(r), int(g), int(b)))
	for i in range(1, espessura // 2):
		n = 0
		linha = frame[posLinha - i]
		for pixel in linha[comeco:fim]:
			r, g, b = pixel
			res[n] = calcSpectre(int(r), int(g), int(b))
			n += 1
		n = 0
		linha = frame[posLinha + i]
		for pixel in linha[comeco:fim]:
			r, g, b = pixel
			res[n] = calcSpectre(int(r), int(g), int(b))
			n += 1

	try:
		medMovVett[contagem] = res
		print(len(medMovVett))
	except:
		medMovVett.append(res)
		print("novo")

	contagem += 1

	res = medMovVett[0]
	for i in range(len(res)):
		try:
			for j in range(1, len(medMovVett)):
				res[i] += medMovVett[j][i] 
		except:
			break

	div = espessura * len(medMovVett) * normalizador
	for i in range(len(res)):
		try:
			res[i] = 100*(1-(res[i]/div)/spectreBlank[i])
		except:
			res[i] = 0

	if not escrevendo:
		escrevendoVett = True
		spectreVett = res
		escrevendoVett = False
	calculandoVett = False


def processLinGBlank(frame, espessura, vezes, pixIni, pixFin):
	global escrevendo
	global calculando
	global spectreBlank
	global contagem
	global medMov
	global normalizador

	if contagem >= vezes: contagem = 0
	if espessura < 1: espessura = 1
	if vezes < 1: vezes = 1
	res = []
	height, width, _ = frame.shape
	posLinha = height // 2

	comeco = pixIni if pixIni < pixFin else pixFin
	fim = pixFin if pixIni < pixFin else pixIni
	linha = frame[posLinha]
	for pixel in linha[comeco : fim]:
		r, g, b = pixel
		res.append(calcSpectre(int(r), int(g), int(b)))
	for i in range(1, espessura // 2):
		n = 0
		linha = frame[posLinha - i]
		for pixel in linha[comeco : fim]:
			r, g, b = pixel
			res[n] = calcSpectre(int(r), int(g), int(b))
			n += 1
		n = 0
		linha = frame[posLinha + i]
		for pixel in linha[comeco : fim]:
			r, g, b = pixel
			res[n] = calcSpectre(int(r), int(g), int(b))
			n += 1

	try:
		medMov[contagem] = res
	except:
		medMov.append(res)

	contagem += 1

	res = medMov[0]
	for i in range(len(res)):
		try:
			for j in range(1, len(medMov)):
				res[i] += medMov[j][i] 
		except:
			break

	div = len(medMov) * espessura
	for i in range(len(res)):
		res[i] = res[i]/div
	normalizador = acharNormalizador(res)
	print(normalizador)

	if not escrevendo:
		escrevendo = True
		spectreBlank = normalizarVet(res)
		escrevendo = False
	calculando = False


def plotSpectreGlobal():
	global escrevendo
	global spectre
	global vetLambdas
	plt.ion() 
	plt.figure(figsize=(8, 6))
	while True:
		if not escrevendo:
			escrevendo = True
			val = spectre
			escrevendo = False
			plt.clf() 
			try:
				plt.plot(vetLambdas, val, color='Red', label='espectro',)
			except:
				plt.plot(val, color='Red', label='espectro',)
			plt.legend()
			plt.xlabel('pixel')
			plt.ylabel('intensidade')
			plt.title('intensidade x pixel')
			plt.grid(True)
			plt.pause(1/10)


def plotSpectreGVett():
	global escrevendoVett
	global spectreVett
	global vetLambdas
	plt.ion() 
	plt.figure(figsize=(8, 6))
	while True:
		if not escrevendoVett:
			escrevendoVett = True
			val = spectreVett
			escrevendoVett = False
			plt.clf()
			try:
				plt.plot(vetLambdas, val, color='Red', label='espectro',)
			except:
				plt.plot(val, color='Red', label='espectro',)
			plt.legend()
			plt.xlabel('pixel')
			plt.ylabel('absorção(%)')
			plt.title('absorção x pixel')
			plt.grid(True)
			plt.ylim(0, 100)
			plt.pause(1/10)


def plotSpectreGlobal1Time():
	global escrevendo
	global spectre
	global vetLambdas
	while (escrevendo):
		time.sleep(1)
	escrevendo = True
	val = spectre
	escrevendo = False
	try:
		plt.plot(vetLambdas, val, color='Red', label='espectro',)
	except:
		plt.plot(val, color='Red', label='espectro',)
	plt.legend()
	plt.xlabel('pixel')
	plt.ylabel('intensidade')
	plt.title('intensidade x pixel')
	plt.grid(True)
	plt.show()


def plotSpectreGVett1Time():
	global escrevendoVett
	global spectreVett
	global vetLambdas
	while (escrevendoVett):
		time.sleep(1)
	escrevendoVett = True
	val = spectreVett
	escrevendoVett = False
	try:
		plt.plot(vetLambdas, val, color='Red', label='espectro',)
	except:
		plt.plot(val, color='Red', label='espectro',)
	plt.legend()
	plt.xlabel('pixel')
	plt.ylabel('intensidade')
	plt.title('intensidade x pixel')
	plt.grid(True)
	plt.show()


def plotSpectreGBlank1Time():
	global escrevendo
	global spectreBlank
	global vetLambdas
	while (escrevendo):
		time.sleep(1)
	escrevendo = True
	val = spectreBlank
	escrevendo = False
	try:
		plt.plot(vetLambdas, val, color='Red', label='espectro',)
	except:
		plt.plot(val, color='Red', label='espectro',)
	plt.legend()
	plt.xlabel('pixel')
	plt.ylabel('intensidade')
	plt.title('intensidade x pixel')
	plt.grid(True)
	plt.show()


def plotSpectre(vet, ax):
	ax.clear()
	ax.plot(vet, color='blue', label='espectro',)
	ax.set_xlabel('Pixel Position')
	ax.set_ylabel('normalized intensity')
	ax.set_title('Spectre intensity x pixel')
	ax.legend()
	ax.grid(True)


def processFrame(frame, ax):
	global calculando
	vet = processLin(frame)
	plotSpectre(vet, ax)
	calculando = False


def grafMedia(streamUrl, espessura, vezes, pixIni, pixFin):
	global calculando
	tentarNovamente = False
	cap = cv2.VideoCapture(streamUrl)

	for x in range(vezes):
		ret, frame = cap.read()
		if not ret:
			print("Error reading frame from stream")
			tentarNovamente = True
			break
		else:
			# Process RGB values
			if not calculando:
				calculando = True
				t1 = threading.Thread(target=processLinGlobal, args=(frame, espessura, vezes, pixIni, pixFin, ))
				t1.start()
				print("foooooooooooooo")

	cap.release()
	cv2.destroyAllWindows()
	if tentarNovamente: grafMedia(streamUrl, espessura, vezes, pixIni, pixFin)
	
	plotSpectreGlobal1Time()


def grafMediaVett(streamUrl, espessura, vezes, pixIni, pixFin):
	global calculandoVett
	tentarNovamente = False
	cap = cv2.VideoCapture(streamUrl)

	for x in range(vezes):
		ret, frame = cap.read()
		if not ret:
			print("Error reading frame from stream")
			tentarNovamente = True
			break
		else:
			# Process RGB values
			if not calculandoVett:
				calculandoVett = True
				t1 = threading.Thread(target=processLinGVett, args=(frame, espessura, vezes, pixIni, pixFin, ))
				t1.start()
				print("foooooooooooooo")

	cap.release()
	cv2.destroyAllWindows()
	if tentarNovamente: grafMediaVett(streamUrl, espessura, vezes, pixIni, pixFin)
	
	plotSpectreGVett1Time()


def grafMediaBlank(streamUrl, espessura, vezes, pixIni, pixFin):
	global calculando
	tentarNovamente = False
	cap = cv2.VideoCapture(streamUrl)

	for x in range(vezes):
		ret, frame = cap.read()
		if not ret:
			print("Error reading frame from stream")
			tentarNovamente = True
			break
		else:
			# Process RGB values
			if not calculando:
				calculando = True
				t1 = threading.Thread(target=processLinGBlank, args=(frame, espessura, vezes, pixIni, pixFin, ))
				t1.start()
				print("blanka")

	cap.release()
	cv2.destroyAllWindows()
	if tentarNovamente: grafMediaBlank(streamUrl, espessura, vezes, pixIni, pixFin)
	
	plotSpectreGBlank1Time()


if __name__ == "__main__":
	operação = 2 # 0=liveVideo; 1=live; 2=media1time
	comBlank = True
	espessura = 10
	sensibilidade = 50
	vezes = 50
	# VAL PARA CALIBRACAO
	posicao = 150
	pixelIni = 64
	pixelFin = 828


	if comBlank:
		print("Insira a cubeta com blank.")
		_ = input("Pressione enter para continuar.")

		grafMediaBlank(stream_url, espessura, vezes, pixelIni, pixelFin)

		print("Retire a cubeta com blank e insira outra com uma amostra.")
		_ = input("Pressione enter para continuar.")
		while True:
			operação = input("Qual operação?\n(1)Video\n(2)live graph\n(3)Foto\n(4)graf 1x\n(q)sair\n-->")
			if operação == '1':
				t0 = threading.Thread(target=streamVideoVett, args=(stream_url, espessura, vezes, pixelIni, pixelFin, ))
				t0.start()
				plotSpectreGVett()
			elif operação == '2':
				t0 = threading.Thread(target=streamGrafVett, args=(stream_url, espessura, vezes, pixelIni, pixelFin, ))
				t0.start()
				plotSpectreGVett()
			elif operação == '3':
				grafMediaVett(stream_url, espessura, vezes, pixelIni, pixelFin)
			elif operação == '4':
				t0 = threading.Thread(target=streamFotoVett, args=(stream_url, espessura, vezes, pixelIni, pixelFin, ))
				t0.start()
				time.sleep(1)
				plotSpectreGVett1Time()
			elif operação == 'q':
				print("tchau")
				break
			else:
				print("erro")

	else:
		while True:
			operação = input("Qual operação?\n(1)video\n(2)live graf\n(3)1 vez\n(4)foto\n(q)sair\n-->")
			if operação == '1':
				t0 = threading.Thread(target=streamVideo, args=(stream_url, espessura, vezes, pixelIni, pixelFin, ))
				t0.start()
				plotSpectreGlobal()
			elif operação == '2':
				t0 = threading.Thread(target=streamGraf, args=(stream_url, espessura, vezes, pixelIni, pixelFin, ))
				t0.start()
				plotSpectreGlobal()
			elif operação == '3':
				grafMedia(stream_url, espessura, vezes)
			elif operação == '4':
				t0 = threading.Thread(target=streamFoto, args=(stream_url, espessura, vezes, pixelIni, pixelFin, ))
				t0.start()
				time.sleep(1/2)
				plotSpectreGlobal1Time()
			elif operação == 'q':
				print("tchau")
				break
			else:
				print("erro")



