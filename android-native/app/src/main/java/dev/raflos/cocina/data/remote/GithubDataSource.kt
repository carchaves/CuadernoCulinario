package dev.raflos.cocina.data.remote

import android.util.Base64
import java.io.IOException

/** El `sha` local quedó viejo (alguien commiteó ese archivo antes que nosotros). Se resuelve
 * releyendo el archivo para tomar el `sha` fresco y reintentando el PUT. */
class GithubConflictException(message: String) : IOException(message)

/** El archivo todavía no existe en el repo (404). */
class GithubNotFoundException(message: String) : IOException(message)

/**
 * Capa fina sobre [GithubContentsApi]: arma la ruta contra el repo configurado y traduce el
 * base64 de la Contents API a/desde el texto JSON que maneja el resto de la app.
 */
class GithubDataSource(
    private val api: GithubContentsApi,
    private val owner: String,
    private val repo: String,
) {

    /** @return el JSON del archivo + el `sha` de su blob (necesario para escribirlo después). */
    suspend fun fetchFile(path: String): Pair<String, String> {
        val file = try {
            api.getFile(owner, repo, path)
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 404) throw GithubNotFoundException("No existe $path en el repo")
            throw e
        }
        val encoded = file.content ?: throw IOException("Respuesta sin contenido para $path")
        return decode(encoded) to file.sha
    }

    /** Commitea [jsonText] en [path]. @return el `sha` nuevo del blob.
     * @throws GithubConflictException si [sha] quedó desactualizado. */
    suspend fun putFile(path: String, jsonText: String, sha: String?, message: String): String {
        val response = api.putFile(
            owner, repo, path,
            GithubPutRequest(message = message, content = encode(jsonText), sha = sha),
        )
        if (!response.isSuccessful) {
            // 409: `sha` viejo. 422: GitHub también lo usa cuando mandamos sha=null sobre un
            // archivo que ya existe (cache local borrada) — se arregla igual, releyendo el sha.
            if (response.code() == 409 || response.code() == 422) {
                throw GithubConflictException("Conflicto de sha al escribir $path")
            }
            throw IOException("GitHub respondió ${response.code()} al escribir $path")
        }
        return response.body()?.content?.sha
            ?: throw IOException("Respuesta sin sha al escribir $path")
    }

    /** Igual que [putFile] pero para binarios (fotos de ticket): se manda el base64 crudo de los
     * bytes en vez del de un texto JSON. */
    suspend fun putBinaryFile(path: String, bytes: ByteArray, sha: String?, message: String): String {
        val response = api.putFile(
            owner, repo, path,
            GithubPutRequest(
                message = message,
                content = Base64.encodeToString(bytes, Base64.NO_WRAP),
                sha = sha,
            ),
        )
        if (!response.isSuccessful) {
            if (response.code() == 409 || response.code() == 422) {
                throw GithubConflictException("Conflicto de sha al escribir $path")
            }
            throw IOException("GitHub respondió ${response.code()} al escribir $path")
        }
        return response.body()?.content?.sha
            ?: throw IOException("Respuesta sin sha al escribir $path")
    }

    /** GitHub devuelve el base64 cortado en líneas de 60 caracteres. */
    private fun decode(content: String): String =
        String(Base64.decode(content.replace("\n", ""), Base64.DEFAULT), Charsets.UTF_8)

    private fun encode(text: String): String =
        Base64.encodeToString(text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
}
