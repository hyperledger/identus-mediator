# [0.3.0](https://github.com/input-output-hk/atala-prism-mediator/compare/prism-mediator-v0.2.0...prism-mediator-v0.3.0) (2023-07-07)


### Bug Fixes

* dynamic DID on the web page ([#40](https://github.com/input-output-hk/atala-prism-mediator/issues/40)) ([0949a19](https://github.com/input-output-hk/atala-prism-mediator/commit/0949a191679caf91575a7d27d69ad1ced89577cd))
* mediator docker-compose image repo and version ([#39](https://github.com/input-output-hk/atala-prism-mediator/issues/39)) ([116174b](https://github.com/input-output-hk/atala-prism-mediator/commit/116174ba616c31f5f28c099dfb1b04a360d258e0))
* mediator rename package and refactoring ([#41](https://github.com/input-output-hk/atala-prism-mediator/issues/41)) ([c755c99](https://github.com/input-output-hk/atala-prism-mediator/commit/c755c99f3547561b46d3b2cbac4e3cecc467d0c6))


### Features

* fix mongo config when no port & support gzip ([#35](https://github.com/input-output-hk/atala-prism-mediator/issues/35)) ([b2b2a02](https://github.com/input-output-hk/atala-prism-mediator/commit/b2b2a02261ffdd5a4362e4b8e28f34479f4eccef))

# [0.2.0](https://github.com/input-output-hk/atala-prism-mediator/compare/prism-mediator-v0.1.1...prism-mediator-v0.2.0) (2023-07-04)


### Bug Fixes

* disabled documentation generation ([#24](https://github.com/input-output-hk/atala-prism-mediator/issues/24)) ([8a2a6ad](https://github.com/input-output-hk/atala-prism-mediator/commit/8a2a6adf712e17caecdbd2b2f06ff5edf68e0a03))
* field name routing_did in keylist response body ([#22](https://github.com/input-output-hk/atala-prism-mediator/issues/22)) ([f61f114](https://github.com/input-output-hk/atala-prism-mediator/commit/f61f1148f033fa3587ab9787bb06428ba14cf6ab))
* Fix release job (add more ram) ([#32](https://github.com/input-output-hk/atala-prism-mediator/issues/32)) ([97ab05a](https://github.com/input-output-hk/atala-prism-mediator/commit/97ab05a943053dce1d789d6aac6d736517c2bfee))
* make content-type header case insensitive & remove support for ws ([#7](https://github.com/input-output-hk/atala-prism-mediator/issues/7)) ([d4a169a](https://github.com/input-output-hk/atala-prism-mediator/commit/d4a169a9ef16b4677bfb31b8785dc79474d9062a))
* mediator db not storing the orignal  value for protected header ([#15](https://github.com/input-output-hk/atala-prism-mediator/issues/15)) ([bd119f1](https://github.com/input-output-hk/atala-prism-mediator/commit/bd119f162b1735d1e7c386e7e421877e19bec7b2))
* mediator unique constraint issue ([#25](https://github.com/input-output-hk/atala-prism-mediator/issues/25)) ([576d7a3](https://github.com/input-output-hk/atala-prism-mediator/commit/576d7a3090598eca325b7c5ddc9834298253ba8b))
* trustPing call back not working ([#23](https://github.com/input-output-hk/atala-prism-mediator/issues/23)) ([e8bf356](https://github.com/input-output-hk/atala-prism-mediator/commit/e8bf356de2b8143e7728e5414e6b2cfc24ae4957))


### Features

* Add config from application.conf and docker-compose and an example ([#10](https://github.com/input-output-hk/atala-prism-mediator/issues/10)) ([2037377](https://github.com/input-output-hk/atala-prism-mediator/commit/203737789a53c9a22d0450564a988518c61f1fc0))
* Add Storage ([#8](https://github.com/input-output-hk/atala-prism-mediator/issues/8)) ([881e66e](https://github.com/input-output-hk/atala-prism-mediator/commit/881e66e9b6d0bbfc49cb0d8ec63583c802257a40))
* mediator  added more logs and associated message Hash and structured logging ([#16](https://github.com/input-output-hk/atala-prism-mediator/issues/16)) ([119b637](https://github.com/input-output-hk/atala-prism-mediator/commit/119b6372ab51ece6ded913b93ea7a607cde9acfe))
* mediator added test for storage layer ([#13](https://github.com/input-output-hk/atala-prism-mediator/issues/13)) ([25728b6](https://github.com/input-output-hk/atala-prism-mediator/commit/25728b6d8aad7cc2143844bad10e2e27bdf5d25f))
* mediator rename package and organise the packages ([#12](https://github.com/input-output-hk/atala-prism-mediator/issues/12)) ([8ea2aac](https://github.com/input-output-hk/atala-prism-mediator/commit/8ea2aaccadf515fe6a7d300c0250d70e38fb3b18))
* new a Webapp to show the QR Code  ([#21](https://github.com/input-output-hk/atala-prism-mediator/issues/21)) ([9af0a87](https://github.com/input-output-hk/atala-prism-mediator/commit/9af0a87cab64b62cb663bbe5bfedf730d09d50de))
* process KeylistQuery and return Keylist ([#30](https://github.com/input-output-hk/atala-prism-mediator/issues/30)) ([c5fb175](https://github.com/input-output-hk/atala-prism-mediator/commit/c5fb17584dc9d464e49589473ee3fe185db0b58f))
* reply to StatusRequest ([#26](https://github.com/input-output-hk/atala-prism-mediator/issues/26)) ([28ee891](https://github.com/input-output-hk/atala-prism-mediator/commit/28ee891999f5174356f245d46812526b044b789b))

## [0.1.1](https://github.com/input-output-hk/atala-prism-mediator/compare/prism-mediator-v0.1.0...prism-mediator-v0.1.1) (2023-05-23)


### Bug Fixes

* execute the ProtocolExecute's jobToRun zio ([#8](https://github.com/input-output-hk/atala-prism-mediator/issues/8)) ([5034499](https://github.com/input-output-hk/atala-prism-mediator/commit/503449991e10a78b82b1094d239703a0c9cd167b))
* Hardcode the atala prism did identity ([#9](https://github.com/input-output-hk/atala-prism-mediator/issues/9)) ([7984eea](https://github.com/input-output-hk/atala-prism-mediator/commit/7984eeacaf6bbd70a58356c3df74ce87b75485bd))

# [0.1.0](https://github.com/input-output-hk/atala-prism-mediator/compare/prism-mediator-v0.0.1...prism-mediator-v0.1.0) (2023-05-18)


### Bug Fixes

* sbt config enable docker plugin ([#6](https://github.com/input-output-hk/atala-prism-mediator/issues/6)) ([a102724](https://github.com/input-output-hk/atala-prism-mediator/commit/a102724bf9ed14f3a51b876c0e6acfbccbc96a6c))


### Features

* Add docker config to build.sbt ([#5](https://github.com/input-output-hk/atala-prism-mediator/issues/5)) ([c7418d0](https://github.com/input-output-hk/atala-prism-mediator/commit/c7418d0b0f17979d20af3ed6d7c8609ec2056c13))
* mediator initial release ([#4](https://github.com/input-output-hk/atala-prism-mediator/issues/4)) ([8499bf2](https://github.com/input-output-hk/atala-prism-mediator/commit/8499bf24d4d94ee30fb917501de4364d2cd6c96e))
