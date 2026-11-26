package com.pedidos360.ms.cupones.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CrearCuponRequest(

        @NotBlank(message = "El codigo es obligatorio")
        @Size(max = 50, message = "El codigo no puede superar 50 caracteres")
        String codigo,

        @Size(max = 255, message = "La descripcion no puede superar 255 caracteres")
        String descripcion,

        @NotNull(message = "El porcentaje de descuento es obligatorio")
        @DecimalMin(value = "0.01", message = "El descuento debe ser mayor que cero")
        @DecimalMax(value = "100.00", message = "El descuento no puede superar el 100%")
        BigDecimal porcentajeDescuento,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        @NotNull(message = "La fecha de vencimiento es obligatoria")
        LocalDate fechaVencimiento,

        @NotNull(message = "El limite de usos es obligatorio")
        @Min(value = 1, message = "El limite de usos debe ser al menos 1")
        Integer limiteUsos

) {
}