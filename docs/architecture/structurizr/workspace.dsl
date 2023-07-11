workspace {

    model {
        agent = softwareSystem "Cloud / Edge Agent" "" "External"

        enterprise "Atala" {
            atalaPrismMediator = softwareSystem "Atala PRISM Mediator" {
                mediatorAgent = container "Mediator Agent" {
                    didCommHandler = component "DIDComm v2 Handler" "Supported protocols:\nBasicMessage 2.0\nMediatorCoordination 2.0\nPickup 3.0\nTrustPing 2.0" "ZIO HTTP" {
                        agent -> this "Sends and receives messages" "HTTPS"
                    }

                    webapp = component "QR Code Web App" "" "Scala.js" {
                        agent -> this "Scans mediation OOB invitation\n(QR code)" "HTTPS"
                    }
                }
                db = container "Mediator Database" "Tables:\nUser Accounts\nMessages Items" "MongoDB" "Database" {
                    didCommHandler -> this "Reads from and writes to"
                }
            }
        }
    }

    views {
        systemContext atalaPrismMediator {
            include *
            autolayout tb
        }

        container atalaPrismMediator {
            include *
            autolayout tb
        }

        component mediatorAgent {
            include *
            autolayout tb
        }

        theme default

        styles {
            element "External" {
                background #999999
                color #ffffff
            }
            element "Database" {
                shape Cylinder
            }
        }
    }

}