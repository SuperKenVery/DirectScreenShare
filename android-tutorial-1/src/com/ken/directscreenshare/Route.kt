package com.ken.directscreenshare

sealed class Route(val route: String) {
    object SelectMode: Route("SelectMode")
    object SelectPeer: Route("SelectPeer")
    object ShareScreen: Route("ShareScreen")
    object ViewScreen: Route("ViewScreen")
}