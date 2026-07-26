# Auditoría MVVM + Retrofit + Room — Costumi (generado del código, 2026-07-26)

> Todo esto se generó listando el código real con `find`, no de memoria. Podés cruzarlo con tu explorador.

## Conteo por capa

| Capa | Qué es | Nº archivos |
|---|---|---|
| **V** ViewModels | *ViewModel.kt | 55 |
| **V** Fragments | pantallas | 57 |
| **V** Adapters | listas RecyclerView | 39 |
| **V** Activity | contenedor único | 1 |
| **V** Layouts XML | res/layout | 143 |
| **M** Repositories | data/repo | 38 |
| **M** APIs a mano | data/remote *Api.kt | 11 |
| **M** Retrofit generado | :api-client apis | 32 |
| **M** DTOs generados | :api-client models | 188 |
| **M** Room (entity/dao/db) | data/local | 5 |

**Total: 246 .kt (app) + 229 .kt (:api-client) + 143 layouts.**

---
## VM — ViewModels (55)
  - ui/SesionViewModel.kt
  - ui/auth/LoginViewModel.kt
  - ui/auth/RecuperarViewModel.kt
  - ui/auth/RegistroViewModel.kt
  - ui/auth/RestablecerViewModel.kt
  - ui/auth/SplashViewModel.kt
  - ui/cliente/carrito/CarritoViewModel.kt
  - ui/cliente/carrito/MisCarritosViewModel.kt
  - ui/cliente/detalle/DetalleDisfrazViewModel.kt
  - ui/cliente/detalle/DetallePrendaViewModel.kt
  - ui/cliente/deudas/MisDeudasViewModel.kt
  - ui/cliente/explorar/ExplorarViewModel.kt
  - ui/cliente/favoritos/FavoritosViewModel.kt
  - ui/cliente/pago/PagoViewModel.kt
  - ui/cliente/pedidos/MisPedidosViewModel.kt
  - ui/cliente/perfil/PerfilViewModel.kt
  - ui/cliente/tienda/TiendaViewModel.kt
  - ui/gestion/GestionShellViewModel.kt
  - ui/gestion/auditoria/AuditoriaViewModel.kt
  - ui/gestion/caja/CajaViewModel.kt
  - ui/gestion/caja/TurnoDetalleViewModel.kt
  - ui/gestion/clientes/ClienteFichaViewModel.kt
  - ui/gestion/clientes/ClienteHistorialViewModel.kt
  - ui/gestion/clientes/ClientesViewModel.kt
  - ui/gestion/config/ConfiguracionViewModel.kt
  - ui/gestion/dashboard/DashboardViewModel.kt
  - ui/gestion/devoluciones/DevolucionFormViewModel.kt
  - ui/gestion/devoluciones/DevolucionesViewModel.kt
  - ui/gestion/disfraces/CategoriasDisfrazViewModel.kt
  - ui/gestion/disfraces/DisfracesViewModel.kt
  - ui/gestion/disfraces/DisfrazAsignarViewModel.kt
  - ui/gestion/disfraces/DisfrazFormViewModel.kt
  - ui/gestion/disfraces/PedidoDisfracesViewModel.kt
  - ui/gestion/empleados/EmpleadosViewModel.kt
  - ui/gestion/empleados/PermisosEmpleadoViewModel.kt
  - ui/gestion/identidad/IdentidadTiendaViewModel.kt
  - ui/gestion/inventario/GruposStockViewModel.kt
  - ui/gestion/inventario/InventarioViewModel.kt
  - ui/gestion/inventario/PrendaFormViewModel.kt
  - ui/gestion/notificaciones/NotificacionesViewModel.kt
  - ui/gestion/pagos/PagoConceptoViewModel.kt
  - ui/gestion/pagos/PagosViewModel.kt
  - ui/gestion/plantillas/PlantillasViewModel.kt
  - ui/gestion/reembolsos/ReembolsosViewModel.kt
  - ui/gestion/reembolsos/SolicitarReembolsoViewModel.kt
  - ui/gestion/rentas/RentaFormViewModel.kt
  - ui/gestion/rentas/RentasViewModel.kt
  - ui/gestion/reportes/ReportesViewModel.kt
  - ui/gestion/sucursales/SucursalesViewModel.kt
  - ui/gestion/taxonomia/CategoriasViewModel.kt
  - ui/gestion/taxonomia/TiposEtiquetaViewModel.kt
  - ui/gestion/taxonomia/ValoresViewModel.kt
  - ui/gestion/ventas/VentaPosViewModel.kt
  - ui/gestion/ventas/VentasViewModel.kt
  - ui/superadmin/SuperAdminViewModel.kt

## V — Fragments (57)
  - ui/auth/LoginFragment.kt
  - ui/auth/RecuperarFragment.kt
  - ui/auth/RegistroFragment.kt
  - ui/auth/RestablecerFragment.kt
  - ui/auth/SplashFragment.kt
  - ui/cliente/ClienteShellFragment.kt
  - ui/cliente/carrito/CarritoFragment.kt
  - ui/cliente/carrito/MisCarritosFragment.kt
  - ui/cliente/detalle/DetalleDisfrazFragment.kt
  - ui/cliente/detalle/DetallePrendaFragment.kt
  - ui/cliente/deudas/MisDeudasFragment.kt
  - ui/cliente/explorar/ExplorarFragment.kt
  - ui/cliente/favoritos/FavoritosFragment.kt
  - ui/cliente/pago/PagoFragment.kt
  - ui/cliente/pedidos/MisPedidosFragment.kt
  - ui/cliente/perfil/PerfilFragment.kt
  - ui/cliente/tienda/TiendaFragment.kt
  - ui/gestion/GestionShellFragment.kt
  - ui/gestion/MasFragment.kt
  - ui/gestion/ProximamenteFragment.kt
  - ui/gestion/auditoria/AuditoriaFragment.kt
  - ui/gestion/caja/CajaFragment.kt
  - ui/gestion/caja/TurnoDetalleFragment.kt
  - ui/gestion/clientes/ClienteFichaFragment.kt
  - ui/gestion/clientes/ClienteHistorialFragment.kt
  - ui/gestion/clientes/ClientesFragment.kt
  - ui/gestion/config/ConfiguracionFragment.kt
  - ui/gestion/dashboard/DashboardFragment.kt
  - ui/gestion/devoluciones/DevolucionFormFragment.kt
  - ui/gestion/devoluciones/DevolucionesFragment.kt
  - ui/gestion/disfraces/CategoriasDisfrazFragment.kt
  - ui/gestion/disfraces/DisfracesFragment.kt
  - ui/gestion/disfraces/DisfrazAsignarFragment.kt
  - ui/gestion/disfraces/DisfrazFormFragment.kt
  - ui/gestion/disfraces/PedidoDisfracesFragment.kt
  - ui/gestion/empleados/EmpleadosFragment.kt
  - ui/gestion/empleados/PermisosEmpleadoFragment.kt
  - ui/gestion/identidad/IdentidadTiendaFragment.kt
  - ui/gestion/inventario/GruposStockFragment.kt
  - ui/gestion/inventario/InventarioFragment.kt
  - ui/gestion/inventario/PrendaFormFragment.kt
  - ui/gestion/notificaciones/NotificacionesFragment.kt
  - ui/gestion/pagos/PagoConceptoFragment.kt
  - ui/gestion/pagos/PagosFragment.kt
  - ui/gestion/plantillas/PlantillasFragment.kt
  - ui/gestion/reembolsos/ReembolsosFragment.kt
  - ui/gestion/reembolsos/SolicitarReembolsoFragment.kt
  - ui/gestion/rentas/RentaFormFragment.kt
  - ui/gestion/rentas/RentasFragment.kt
  - ui/gestion/reportes/ReportesFragment.kt
  - ui/gestion/sucursales/SucursalesFragment.kt
  - ui/gestion/taxonomia/CategoriasFragment.kt
  - ui/gestion/taxonomia/TiposEtiquetaFragment.kt
  - ui/gestion/taxonomia/ValoresFragment.kt
  - ui/gestion/ventas/VentaPosFragment.kt
  - ui/gestion/ventas/VentasFragment.kt
  - ui/superadmin/SuperAdminHomeFragment.kt

## V — Adapters (39)
  - ui/cliente/carrito/CarritoAbiertoAdapter.kt
  - ui/cliente/carrito/CarritoLineaAdapter.kt
  - ui/cliente/detalle/OpcionGrillaAdapter.kt
  - ui/cliente/detalle/SlotOpcionAdapter.kt
  - ui/cliente/deudas/DeudaAdapter.kt
  - ui/cliente/explorar/DestacadoAdapter.kt
  - ui/cliente/explorar/EmpresaAdapter.kt
  - ui/cliente/favoritos/FavoritoAdapter.kt
  - ui/cliente/pedidos/LineaPedidoAdapter.kt
  - ui/cliente/pedidos/PedidoAdapter.kt
  - ui/cliente/tienda/DisfrazVitrinaAdapter.kt
  - ui/cliente/tienda/PrendaAdapter.kt
  - ui/common/PrendaCatalogoGrillaAdapter.kt
  - ui/gestion/auditoria/AuditoriaAdapter.kt
  - ui/gestion/caja/MovimientoAdapter.kt
  - ui/gestion/caja/TurnoAdapter.kt
  - ui/gestion/clientes/ClienteAdapter.kt
  - ui/gestion/clientes/HistorialAdapter.kt
  - ui/gestion/devoluciones/DevolucionAdapter.kt
  - ui/gestion/disfraces/CategoriaDisfrazAdapter.kt
  - ui/gestion/disfraces/DisfrazAdapter.kt
  - ui/gestion/empleados/EmpleadoAdapter.kt
  - ui/gestion/empleados/PermisoAdapter.kt
  - ui/gestion/inventario/CatalogoStockAdapter.kt
  - ui/gestion/inventario/GrupoStockAdapter.kt
  - ui/gestion/inventario/PrendaAdapter.kt
  - ui/gestion/inventario/PrendasLoadStateAdapter.kt
  - ui/gestion/notificaciones/NotificacionAdapter.kt
  - ui/gestion/pagos/OperacionPagoAdapter.kt
  - ui/gestion/pagos/PagoAdapter.kt
  - ui/gestion/plantillas/PlantillaAdapter.kt
  - ui/gestion/reembolsos/ReembolsoAdapter.kt
  - ui/gestion/rentas/RentaAdapter.kt
  - ui/gestion/sucursales/SucursalAdapter.kt
  - ui/gestion/taxonomia/CategoriaAdapter.kt
  - ui/gestion/taxonomia/TipoEtiquetaAdapter.kt
  - ui/gestion/taxonomia/ValorEtiquetaAdapter.kt
  - ui/gestion/ventas/VentaAdapter.kt
  - ui/superadmin/PanelSuperAdminAdapter.kt

## V — Activity + UI común (StateView, componentes reutilizables)
  - MainActivity.kt
  - ui/common/Fechas.kt
  - ui/common/FiltroCompacto.kt
  - ui/common/LineasDeArticulos.kt
  - ui/common/ListaBuscable.kt
  - ui/common/Pastilla.kt
  - ui/common/PrendaCatalogoGrillaAdapter.kt
  - ui/common/SelectorCatalogo.kt
  - ui/common/SelectorDeCantidad.kt
  - ui/common/SelectorDePeriodo.kt
  - ui/common/SelectorDisfraces.kt
  - ui/common/StateView.kt

## M — Repositories (38)
  - data/repo/AuditoriaRepository.kt
  - data/repo/AuthRepository.kt
  - data/repo/CajaRepository.kt
  - data/repo/ClientesPagingSource.kt
  - data/repo/ClientesRepository.kt
  - data/repo/ConfiguracionRepository.kt
  - data/repo/ContextoGestionRepository.kt
  - data/repo/CuentaRepository.kt
  - data/repo/DevolucionRepository.kt
  - data/repo/DisfrazRepository.kt
  - data/repo/EmpleadoRepository.kt
  - data/repo/FavoritosRepository.kt
  - data/repo/GruposStockRepository.kt
  - data/repo/InventarioRepository.kt
  - data/repo/InvitacionRepository.kt
  - data/repo/Mapeadores.kt
  - data/repo/MarketplaceRepository.kt
  - data/repo/MembresiaRepository.kt
  - data/repo/MiEmpresaRepository.kt
  - data/repo/MisDeudasRepository.kt
  - data/repo/NotificacionRepository.kt
  - data/repo/PaginaRemotaPagingSource.kt
  - data/repo/PagoClienteRepository.kt
  - data/repo/PagoRepository.kt
  - data/repo/PedidoRepository.kt
  - data/repo/PerfilRepository.kt
  - data/repo/PlantillaRepository.kt
  - data/repo/PrendasPagingSource.kt
  - data/repo/PushRepository.kt
  - data/repo/ReembolsoRepository.kt
  - data/repo/RentaRepository.kt
  - data/repo/RentasPagingSource.kt
  - data/repo/ReporteRepository.kt
  - data/repo/SucursalRepository.kt
  - data/repo/SuperAdminRepository.kt
  - data/repo/TaxonomiaRepository.kt
  - data/repo/VentaRepository.kt
  - data/repo/VentasPagingSource.kt

## M — Red / Retrofit (data/remote: APIs a mano, interceptores, sesión, ejecutor)
  - data/remote/ComprobantePagoApi.kt
  - data/remote/ContratoRentaApi.kt
  - data/remote/EjecutorDeLlamadas.kt
  - data/remote/FotoDisfrazApi.kt
  - data/remote/FotoEmpresaApi.kt
  - data/remote/FotoPerfilApi.kt
  - data/remote/FotoPrendaApi.kt
  - data/remote/FotoSucursalApi.kt
  - data/remote/MiEmpresaApi.kt
  - data/remote/MisCarritosApi.kt
  - data/remote/MisDeudasApi.kt
  - data/remote/ReporteExportApi.kt
  - data/remote/interceptor/AuthInterceptor.kt
  - data/remote/interceptor/TokenAuthenticator.kt
  - data/remote/session/EventosDeSesion.kt
  - data/remote/session/SesionLocal.kt

## M — Room (data/local)
  - data/local/CostumiDatabase.kt
  - data/local/dao/EmpresaDao.kt
  - data/local/dao/FavoritoDao.kt
  - data/local/entity/EmpresaEntity.kt
  - data/local/entity/FavoritoDisfrazEntity.kt

## M — DI / Core (Hilt modules, DispatcherProvider, RespuestaRed, UiState)
  - core/DispatcherProvider.kt
  - core/Formatos.kt
  - core/RespuestaRed.kt
  - core/Rol.kt
  - core/UiState.kt
  - di/CoreModule.kt
  - di/DatabaseModule.kt
  - di/NetworkModule.kt

## M — :api-client (Retrofit generado desde el contrato del backend)
APIs Retrofit (32):
  - ActividadDeEmpleadoControllerApi.kt
  - AuditoriaControllerApi.kt
  - AuthControllerApi.kt
  - CajaControllerApi.kt
  - CarritoControllerApi.kt
  - CategoriaControllerApi.kt
  - CategoriaDeDisfrazControllerApi.kt
  - ClienteControllerApi.kt
  - ConfiguracionControllerApi.kt
  - ConteoDeDependenciasControllerApi.kt
  - DevolucionControllerApi.kt
  - DisfrazControllerApi.kt
  - DisfrazMarketplaceControllerApi.kt
  - EmpleadoControllerApi.kt
  - EmpresaControllerApi.kt
  - GrupoDeStockControllerApi.kt
  - InvitacionControllerApi.kt
  - MarketplaceControllerApi.kt
  - MembresiaControllerApi.kt
  - MisPermisosControllerApi.kt
  - NotificacionControllerApi.kt
  - PagoControllerApi.kt
  - PerfilControllerApi.kt
  - PermisosEmpleadoControllerApi.kt
  - PlantillaNotificacionControllerApi.kt
  - PrendaControllerApi.kt
  - ReembolsoControllerApi.kt
  - RentaControllerApi.kt
  - ReporteControllerApi.kt
  - SucursalControllerApi.kt
  - TipoEtiquetaControllerApi.kt
  - VentaControllerApi.kt

DTOs/models: 188 archivos (generados, uno por request/response del backend — no se listan uno por uno).
