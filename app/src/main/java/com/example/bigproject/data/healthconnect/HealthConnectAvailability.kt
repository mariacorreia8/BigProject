package com.example.bigproject.data.healthconnect

sealed class HealthConnectAvailability {

    //Health Connect não existe neste dispositivo(sem suporte)
    data object NotSupported : HealthConnectAvailability()

    //Health Connect existe, mas não está instalado
    data object NotInstalled : HealthConnectAvailability()

    //Health Connect está instalado mas a app não tem as permissões necessárias
    data  object PermissionsNotGranted : HealthConnectAvailability()

    // app tem acesso ao Health Connect e pode ler os dados
    data object InstalledAndAvailable : HealthConnectAvailability()
}