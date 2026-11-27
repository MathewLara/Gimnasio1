package com.mathew.gimnasio.controladores;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.json.JSONArray;
import org.json.JSONObject;

import com.mathew.gimnasio.modelos.SolicitudIaDTO;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/ia")
public class IaController {

    private static final String[] GEMINI_MODELS = {
            "gemini-flash-lite-latest",
            "gemini-flash-latest",
            "gemini-2.5-flash",
            "gemma-4-26b-a4b-it",
            "gemma-4-31b-it"
    };

    @POST
    @Path("/generar-plan")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generarPlan(SolicitudIaDTO datos) {
        try {
            String apiKey = System.getenv("GEMINI_API_KEY");
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return Response.status(500)
                        .entity(new JSONObject().put("mensaje", "Falta configurar la variable de entorno GEMINI_API_KEY en Render.").toString())
                        .build();
            }

            String prompt = String.format(
                    "Actúa como un entrenador personal y nutricionista deportivo de élite del gimnasio Iron Fitness. " +
                            "Crea un plan de %s para una persona con: Edad %d años, Peso %.1f kg, Altura %.1f cm, Meta: %s. " +
                            "REGLAS DE DISEÑO ESTRICTAS: " +
                            "1. Responde SOLO con código HTML limpio usando Bootstrap 5 (sin etiquetas <html>, <head>, <body> ni bloques ```html). " +
                            "2. Prohibido escribir párrafos largos. Usa textos cortos, directos y escaneables. " +
                            "3. Empieza con una fila de 3 mini-tarjetas KPI (<div class='row g-2 mb-4'>) mostrando: IMC aproximado, Calorías diarias sugeridas y Enfoque principal, usando tarjetas con clase 'bg-black border border-secondary rounded p-2 text-center' y valores en 'text-warning fw-bold fs-5'. " +
                            "4. Organiza el resto del plan en tarjetas separadas por día o por comida usando: " +
                            "<div class='card bg-black border-secondary mb-3 ia-card'><div class='card-header border-secondary d-flex justify-content-between align-items-center'><span class='fw-bold text-warning'><i class='bi bi-lightning-charge-fill me-2'></i>TÍTULO</span><span class='badge bg-warning text-dark'>ETIQUETA</span></div><div class='card-body'><ul class='list-group list-group-flush bg-transparent'>...</ul></div></div>. " +
                            "5. Cada elemento de la lista debe tener clase 'list-group-item bg-transparent text-light border-secondary px-0 py-2 d-flex justify-content-between align-items-center' poniendo el ejercicio/alimento a la izquierda y las series/reps o porción a la derecha dentro de un <span class='badge bg-dark border border-warning text-warning'>. " +
                            "6. Termina con un consejo motivacional corto dentro de un <div class='alert alert-warning bg-dark text-warning border-warning mt-3 mb-0'><i class='bi bi-trophy-fill me-2'></i>...</div>.",
                    datos.getTipoPlan(), datos.getEdad(), datos.getPeso(), datos.getAltura(), datos.getMeta()
            );

            JSONObject part = new JSONObject().put("text", prompt);
            JSONObject content = new JSONObject().put("parts", new JSONArray().put(part));
            JSONObject body = new JSONObject().put("contents", new JSONArray().put(content));

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = null;
            String ultimoError = "No se obtuvo respuesta de la API de IA.";

            for (String model : GEMINI_MODELS) {
                try {
                    String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey.trim();

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(geminiUrl))
                            .timeout(Duration.ofSeconds(25))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                            .build();

                    response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        break;
                    } else {
                        ultimoError = "Modelo " + model + " respondió " + response.statusCode() + ": " + response.body();
                        System.err.println(ultimoError);
                    }
                } catch (Exception ex) {
                    ultimoError = "Error consultando modelo " + model + ": " + ex.getMessage();
                    System.err.println(ultimoError);
                }
            }

            if (response != null && response.statusCode() == 200) {
                JSONObject jsonRes = new JSONObject(response.body());
                String textoIA = jsonRes.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");

                textoIA = textoIA.replace("```html", "").replace("```", "").trim();

                JSONObject respuesta = new JSONObject().put("recomendacion", textoIA);
                return Response.ok(respuesta.toString()).build();
            } else {
                return Response.status(500)
                        .entity(new JSONObject().put("mensaje", "Error en la API de IA: " + ultimoError).toString())
                        .build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500)
                    .entity(new JSONObject().put("mensaje", "Error interno: " + e.getMessage()).toString())
                    .build();
        }
    }
}