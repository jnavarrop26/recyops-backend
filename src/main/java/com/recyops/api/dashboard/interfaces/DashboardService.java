package com.recyops.api.dashboard.interfaces;

import com.recyops.api.dashboard.dtos.RespuestaActividadDia;
import com.recyops.api.dashboard.dtos.RespuestaResumenDashboard;
import java.util.List;

public interface DashboardService {

    RespuestaResumenDashboard resumen();

    /** Actividad de los ultimos N dias (incluye dias sin movimiento), del mas reciente al mas antiguo. */
    List<RespuestaActividadDia> actividadDiaria(int dias);
}
