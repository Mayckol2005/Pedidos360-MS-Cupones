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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cupones")
public class CuponController {

    private final CuponService cuponService;

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

    // Aplicar cupón y registrar un uso
    // Operación administrativa: no llamar desde Angular.
    // Pendiente de vincular a una orden antes de su uso real.
    @PostMapping("/aplicar")
    public ResultadoDescuento aplicar(
            @Valid @RequestBody SolicitudDescuento request) {

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