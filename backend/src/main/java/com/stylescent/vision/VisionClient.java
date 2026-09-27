package com.stylescent.vision;

import com.stylescent.exception.VisionNoDisponibleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

// Cliente HTTP del microservicio de visión (vision-service/, FastAPI).
// Le mandamos la foto junto con los nombres del catálogo: así el servicio no
// necesita acceso a la BD y reconoce al momento cualquier categoría/color/estilo nuevo.
@Component
public class VisionClient {

    private final RestClient restClient;

    public VisionClient(@Value("${vision.url}") String visionUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                // Por defecto el cliente de Java intenta subir a HTTP/2 ("Upgrade: h2c")
                // y uvicorn descarta el cuerpo de esas peticiones: FastAPI respondía 422
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        // La primera petición tras arrancar es más lenta (CLIP calienta); de sobra con 30 s
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .baseUrl(visionUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public AnalisisVision analizar(byte[] imagen, String nombreArchivo,
                                   List<String> categorias, List<String> colores, List<String> estilos) {
        MultiValueMap<String, Object> formulario = new LinkedMultiValueMap<>();
        formulario.add("imagen", new ByteArrayResource(imagen) {
            // Sin nombre de archivo, la parte no se envía como fichero y FastAPI la rechaza
            @Override
            public String getFilename() {
                return nombreArchivo;
            }
        });
        categorias.forEach(c -> formulario.add("categorias", c));
        colores.forEach(c -> formulario.add("colores", c));
        estilos.forEach(e -> formulario.add("estilos", e));

        try {
            return restClient.post()
                    .uri("/analizar")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(formulario)
                    .retrieve()
                    .body(AnalisisVision.class);
        } catch (HttpClientErrorException e) {
            // 400/413: la foto no vale (no es una imagen, demasiado grande...): error del usuario
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST || e.getStatusCode() == HttpStatus.CONTENT_TOO_LARGE) {
                throw new IllegalArgumentException("La imagen no se pudo analizar: " + detalle(e));
            }
            throw new VisionNoDisponibleException("El servicio de visión rechazó la petición", e);
        } catch (RestClientException e) {
            // Caído, sin arrancar o respondiendo 5xx
            throw new VisionNoDisponibleException("El servicio de visión no está disponible", e);
        }
    }

    // FastAPI devuelve {"detail": "..."}; si no se puede leer, el cuerpo entero
    private static String detalle(HttpClientErrorException e) {
        String cuerpo = e.getResponseBodyAsString();
        int inicio = cuerpo.indexOf("\"detail\":\"");
        if (inicio < 0) {
            return cuerpo;
        }
        int desde = inicio + "\"detail\":\"".length();
        int hasta = cuerpo.indexOf('"', desde);
        return hasta > desde ? cuerpo.substring(desde, hasta) : cuerpo;
    }
}
