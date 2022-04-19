package sp.service.sample

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.pow

fun DatagramSocket.send(
	host: String,
	port: Int,
	buffer: ByteArray,
	offset: Int = 0,
	length: Int = buffer.size
) {
	val packet = DatagramPacket(buffer, offset, length, InetAddress.getByName(host), port)
	send(packet)
}

fun DatagramSocket.receive(
	buffer: ByteArray,
	offset: Int = 0,
	length: Int = buffer.size
): DatagramPacket {
	val result = DatagramPacket(buffer, offset ,length)
	receive(result)
	return result
}

fun DatagramSocket.receive(size: Int = 1024): DatagramPacket {
	return receive(buffer = ByteArray(size))
}

private fun getHostAddress(): String {
	return NetworkInterface.getNetworkInterfaces().toList()
		.flatMap { it.inetAddresses.toList() }
		.filterIsInstance<Inet4Address>()
		.filterNot { it.isLoopbackAddress }
		.single().hostAddress
}

private fun ByteArray.toHexString(): String {
	val chars = "0123456789abcdef"
	val builder = StringBuilder(size * 2)
	forEach {
		val i = it.toInt()
		builder
			.append(chars[i shr 4 and 0x0f])
			.append(chars[i and 0x0f])
	}
	return builder.toString()
}

private fun getAuthorizationDigest(
	algorithm: String,
	uri: String,
	realm: String,
	nonce: String,
	method: String,
	name: String,
	password: String
): String {
	val result = mutableMapOf(
		"username" to name,
		"realm" to realm,
		"nonce" to nonce,
		"uri" to uri
	)
	val uDigest = MessageDigest.getInstance(algorithm).digest(
		"$name:$realm:$password".toByteArray(Charsets.UTF_8)
	).toHexString()
	val mDigest = MessageDigest.getInstance(algorithm).digest(
		"$method:$uri".toByteArray(Charsets.UTF_8)
	).toHexString()
	TODO()
}

private fun Int.toTwoBytes(): ByteArray {
	val result = ByteArray(2)
	if (this > 2.0.pow(31)) error("Integer value $this > 2^31")
	if (this < 0) error("Integer value $this < 0")
	result[0] = ((this shr 8) and 0xFF).toByte()
	result[1] = (this and 0xFF).toByte()
	return result
}

private fun ByteArray.twoToInteger(): Int {
	check(size > 1)
	val f = (this[0].toInt() and 0xFF) shl 8
	val s = this[1].toInt() and 0xFF
	return f + s
}

private fun Byte.toInteger(): Int {
	return toInt() and 0xFF
}

private fun parseAddress(array: ByteArray): Pair<String, Int> {
	if (array.size < 8) error("Data array too short")
	val family = array[1].toInteger()
	if (family != 0x01) error("Family $family is not supported")
	val port = ByteArray(16).also {
		System.arraycopy(array, 2, it, 0, 2)
	}.twoToInteger()
	val address = listOf(4, 5, 6, 7)
		.joinToString(separator = ".") {
			array[it].toInteger().toString()
		}
	return address to port
}

fun ByteArray.getMappedAddress(): Pair<String, Int> {
	val lengthArray = ByteArray(2).also {
		System.arraycopy(this, 2, it, 0, 2)
	}
	var offset = 20
	var length = lengthArray.twoToInteger()
	while (length > 0) {
		val tmpArray = ByteArray(length).also {
			System.arraycopy(this, offset, it, 0, length)
		}
		val type = ByteArray(2).also {
			System.arraycopy(tmpArray, 0, it, 0, 2)
		}.twoToInteger()
		val valueArrayLength = ByteArray(2).also {
			System.arraycopy(tmpArray, 2, it, 0, 2)
		}.twoToInteger()
		val valueArray = ByteArray(valueArrayLength).also {
			System.arraycopy(tmpArray, 4, it, 0, it.size)
		}
		val d = valueArray.size + 4
		when (type) {
			0x0001 -> {
				return parseAddress(valueArray)
			}
		}
		length -= d
		offset += d
	}
	TODO()
}

fun main(args: Array<String>) {
	val arguments = args.single().split(",").associate {
		val array = it.split("=")
		check(array.size == 2)
		array[0] to array[1]
	}
	val rHost = arguments["rh"]!!
	val rPort = arguments["rp"]!!.toInt()
	val fName = arguments["fn"]!!
	val fPassword = arguments["fp"]!!
	val tName = arguments["tn"]!!
	val sServer = arguments["ss"]!!
	DatagramSocket().use { socket ->
		socket.soTimeout = 5_000
		socket.connect(InetAddress.getByName(rHost), rPort)
//		val buffer = ByteArray(20).also {
//			val type = 0x0001
//			System.arraycopy(type.toTwoBytes(), 0, it, 0, 2)
//			System.arraycopy(0.toTwoBytes(), 0, it, 2, 2)
//		}
//		socket.send(host = sServer, port = 3478, buffer = buffer)
//		val (lHost, lPort) = socket.receive().data.getMappedAddress()
		val lHost = getHostAddress()
		val lPort = socket.localPort
		println("local host: $lHost")
		println("local port: $lPort")
		val number = 1
		val version = "2.0"
		val method = "REGISTER"
		val branch = "z9hG4bK" + UUID.randomUUID().toString()
		val callId = UUID.randomUUID().toString()
		val tag = UUID.randomUUID().toString()
		val request = """
			$method sip:$rHost SIP/$version
			Via: SIP/$version/UDP $lHost:$lPort;branch=$branch
			Call-ID: $callId
			CSeq: $number $method
			To: sip:${fName}@${rHost}
			From: sip:${fName}@${rHost};tag=$tag
			Contact: sip:$fName@$lHost:$lPort
		""".trimIndent().split("\n").joinToString(separator = "\r\n", postfix = "\r\n\r\n")
		println("request:\n\t---\n$request\n\t---")
		socket.send(host = rHost, port = rPort, buffer = request.toByteArray())
		println("\nwait response...")
		val response = String(socket.receive().data)
		println("response:\n\t---\n$response\n\t---")
		socket.disconnect()
	}
}
