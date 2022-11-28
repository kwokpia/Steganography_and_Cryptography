package cryptography

import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

const val MSG_NOT_FOUND = "Message not found!"
const val UI_PROMPT = "Task (hide, show, exit):"
const val CMD_EXIT = "exit"
const val CMD_HIDE = "hide"
const val CMD_SHOW = "show"
const val MSG_EXIT = "Bye!"
const val MSG_INPUT_FILENAME = "Input image file:"
const val MSG_OUTPUT_FILENAME = "Output image file:"
const val MSG_TO_HIDE = "Message to hide:"
const val MSG_IMAGE_TOO_SMALL = "The input image is not large enough to hold this message."
const val MSG_SHOW = "Message:"
const val MSG_PASSWORD = "Password:"

const val END_SIGN_MESSAGE = "000000000000000000000011"
const val IMG_TYPE = "png"

fun main() {
    do {
        println(UI_PROMPT)
        val userInput = readln().lowercase()
        when (userInput) {
            CMD_HIDE -> hide()
            CMD_SHOW -> show()
            CMD_EXIT -> println(MSG_EXIT)
        }

    } while (userInput != CMD_EXIT)
}

fun getUserInput(promptMessage: String): String {
    println(promptMessage)
    return readln()
}

fun Byte.toBinary8String(): String {
    val byteBinary = this.toString(2)
    return "0".repeat(Byte.SIZE_BITS - byteBinary.length) + byteBinary

}

fun encodeMessageToBits(message: String): List<Int> {
    return (message).encodeToByteArray()
        .map { eachByte ->
            eachByte.toBinary8String().map {
                it.digitToInt()
            }
        }.flatten()
}

fun isImgTooSmall(inImgImage: BufferedImage, messageToBits: List<Int>): Boolean {
    return (messageToBits.size > inImgImage.width * inImgImage.height)
}

fun hide() {
    val inImgFile = File(getUserInput(MSG_INPUT_FILENAME))
    val outImgFile = File(getUserInput(MSG_OUTPUT_FILENAME))
    val messageToBits = encrypt(
        encodeMessageToBits(getUserInput(MSG_TO_HIDE)),
        encodeMessageToBits(getUserInput(MSG_PASSWORD))
    ) + END_SIGN_MESSAGE.map { it.digitToInt() }

    val inImgImage: BufferedImage
    try {
        inImgImage = ImageIO.read(inImgFile)
    } catch (e: IOException) {
        println(e.message)
        return
    }

    if (isImgTooSmall(inImgImage, messageToBits)) {
        println(MSG_IMAGE_TOO_SMALL)
        return
    }

    try {
        ImageIO.write(imageWithMessage(inImgImage, messageToBits), IMG_TYPE, outImgFile)
        println("Message saved in ${outImgFile.name} image.")
    } catch (e: IOException) {
        println(e.message)
    }

}

fun encrypt(message: List<Int>, password: List<Int>): List<Int> {
   return message.mapIndexed { index, i ->
        i xor password[index % password.size]
    }
}

fun imageWithMessage(img: BufferedImage, message: List<Int>): BufferedImage {
    val imgWidth = img.width
    val imgHeight = img.height
    var bitCount = 0

    look@ for (y in 0 until imgHeight) {
        for (x in 0 until imgWidth) {
            if (bitCount == message.size)
                break@look
            val rgb = img.getRGB(x, y)
            if (rgb and 1 != message[bitCount])
                img.setRGB(x, y, rgb xor 1)
            bitCount++
        }
    }
    return img
}

fun show() {
    val inImgFile = File(getUserInput(MSG_INPUT_FILENAME))
    val password = getUserInput(MSG_PASSWORD)
    val img: BufferedImage

    try {
        img = ImageIO.read(inImgFile)
    } catch (e: IOException) {
        println(e.message)
        return
    }
    println("$MSG_SHOW\n${messageFromImage(img,encodeMessageToBits(password))}")
}

fun decrypt(message: List<Int>, password: List<Int>): List<Int> {
    return  encrypt(message,password)
}

fun messageFromImage(img: BufferedImage,password: List<Int>): String {
    val imgWidth = img.width
    val imgHeight = img.height
    val bitList = mutableListOf<Int>()

    for (y in 0 until imgHeight) {
        for (x in 0 until imgWidth) {
            if (END_SIGN_MESSAGE in bitList.joinToString("")) {
                return decodeFromBits(decrypt(bitList.dropLast(END_SIGN_MESSAGE.length),password))
            }

            bitList.add(img.getRGB(x, y) and 1)
        }

    }
    return MSG_NOT_FOUND
}

fun decodeFromBits(bits: List<Int>): String {
    return bits.windowed(Byte.SIZE_BITS, Byte.SIZE_BITS) {
        it
            .joinToString("")
            .toByte(2)
    }
        .toByteArray()
        .toString(Charsets.UTF_8)
}



