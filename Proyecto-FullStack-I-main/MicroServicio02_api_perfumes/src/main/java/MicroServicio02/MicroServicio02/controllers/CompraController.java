package MicroServicio02.MicroServicio02.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.reactive.function.client.WebClient;

import MicroServicio02.MicroServicio02.models.CompraRequest;
import MicroServicio02.MicroServicio02.models.CompraResponse;
import MicroServicio02.MicroServicio02.models.Perfume;
import MicroServicio02.MicroServicio02.models.User;
import MicroServicio02.MicroServicio02.services.PerfumeService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
@RestController
@RequestMapping("compra")
public class CompraController {
    @Autowired
    private PerfumeService perfumeService;

    @Autowired
    private WebClient webClient;

    @PostMapping("/comprar")
    public CompraResponse comprar(@Valid @RequestBody CompraRequest compraRequest) {
        
        CompraResponse response = new CompraResponse();
    
        try { 
            Long idPerfume = Long.parseLong(compraRequest.getIdPerfume());
            Perfume Per = perfumeService.obtenerUno(idPerfume);
            if (Per == null) throw new Exception("Perfume no encontrado");

            // Validar stock
            if (Per.getStock() <= 0) {
                throw new Exception("No hay stock disponible");
            }

            // Obtener usuario desde microservicio usuarios
            User usuario = webClient
                .get()
                .uri("http://localhost:8081/usuario/" + compraRequest.getIdUsuario())
                .retrieve()
                .bodyToMono(User.class)
                .block();

            if (usuario == null) throw new Exception("Usuario no encontrado");

            // Restar stock y guardar
            Per.setStock(Per.getStock() - 1);
            perfumeService.agregar(Per); // actualiza el stock

            webClient.post()
            .uri("http://localhost:8084/compras")
            .bodyValue(compraRequest) // puedes enviar CompraRequest si el microservicio acepta esos campos
            .retrieve()
            .bodyToMono(Void.class)  // si el microservicio devuelve algo puedes usar Compra.class
            .block();
           

            // Armar respuesta
            response.setIdBoleta("Compra exitosa. Perfume ID: " + Per.getId() + " Correo usuario: " + usuario.getEmail());
            response.setStockRestante(Per.getStock());
            response.setExito(true);

        } catch (Exception e) {
            response.setExito(false);
            response.setMensaje("Error al procesar la compra: " + e.getMessage());
        }

        return response;
    }
}
