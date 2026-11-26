package com.pedidos360.ms.cupones.controller;

import com.pedidos360.ms.cupones.dto.CrearCuponRequest;
import com.pedidos360.ms.cupones.entity.Cupon;
import com.pedidos360.ms.cupones.service.CuponService;
import com.pedidos360.ms.cupones.service.CuponService.ResultadoDescuento;


import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cupones")
public class CuponController {

    private final CuponService cuponService;

    @Value("${cupones.clave-interna}")
    private String claveInterna;

    public CuponController(CuponService cuponService) {
        this.cuponService = cuponService;
    }

    // Crear un cupón
    @PostMapping
    public ResponseEntity<Cupon> crear(
            @Valid @RequestBody CrearCuponRequest request) {

        Cupon cupon = new Cupon();

        cupon.setCodigo(request.codigo());
        cupon.setDescripcion(request.descripcion());
        cupon.setPorcentajeDescuento(
                request.porcentajeDescuento()
        );
        cupon.setFechaInicio(request.fechaInicio());
        cupon.setFechaVencimiento(
                request.fechaVencimiento()
        );
        cupon.setLimiteUsos(request.limiteUsos());

        Cupon creado = cuponService.crear(cupon);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(creado);
    }

    // Listar todos los cupones
    @GetMapping
    public List<Cupon> listar() {
        return cuponService.listar();
    }

    // Buscar un cupón por ID
    @GetMapping("/{id}")
    public Cupon buscar(@PathVariable Long id) {
        return cuponService.buscarPorId(id);
    }

    // Validar un cupón
    @GetMapping("/validar/{codigo}")
    public Cupon validar(@PathVariable String codigo) {
        return cuponService.validar(codigo);
    }

    // Calcular descuento sin consumir el cupón
    @PostMapping("/calcular")
    public ResultadoDescuento calcular(
            @Valid @RequestBody SolicitudDescuento request) {

        return cuponService.calcularDescuento(
                request.codigo(),
                request.subtotal()
        );
    }

    // Aplicar cupón y registrar un uso.
    // Solo lo llama MS-Orders al crear una orden (lleva la clave interna).
    @PostMapping("/aplicar")
    public ResultadoDescuento aplicar(
            @RequestHeader(value = "X-Clave-Interna", required = false) String clave,
            @Valid @RequestBody SolicitudDescuento request) {

        if (clave == null || !MessageDigest.isEqual(
                clave.getBytes(StandardCharsets.UTF_8),
                claveInterna.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Operacion reservada a MS-Orders");
        }

        return cuponService.aplicarCupon(
                request.codigo(),
                request.subtotal()
        );
    }

    // Desactivar un cupón
    @PatchMapping("/{id}/desactivar")
    public Cupon desactivar(@PathVariable Long id) {
        return cuponService.desactivar(id);
    }

    // Datos recibidos para calcular un descuento
    public record SolicitudDescuento(
            @NotBlank
            String codigo,

            @NotNull
            @DecimalMin(value = "0.0", inclusive = false)
            BigDecimal subtotal
    ) {
    }

    // Manejar errores de validación de negocio
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarError(
            IllegalArgumentException ex) {

        return ResponseEntity
                .badRequest()
                .body(Map.of("mensaje", ex.getMessage()));
    }
}