package com.rpgstats.compat;

import com.rpgstats.integration.IntegrationServices;

/** Serviços permitidos aos adaptadores; todas as regras continuam no core. */
public record CompatContext(IntegrationServices services) {}
