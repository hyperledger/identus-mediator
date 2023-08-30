## [0.9.2](https://github.com/input-output-hk/atala-prism-mediator/compare/prism-mediator-v0.9.1...prism-mediator-v0.9.2) (2023-08-30)


### Bug Fixes

* **mediator:** Enabled X-Request-Id ATL-5568 ([#104](https://github.com/input-output-hk/atala-prism-mediator/issues/104)) ([b0d4fee](https://github.com/input-output-hk/atala-prism-mediator/commit/b0d4feec19b3d4dc796e580789b163d2c3855d55))
* mongodb init script updated infrastructure mongodb yaml ([#94](https://github.com/input-output-hk/atala-prism-mediator/issues/94)) ([d9cc42a](https://github.com/input-output-hk/atala-prism-mediator/commit/d9cc42a319d505e37b5a9cdbe47802283537adaf))
* set default endpoint to localhost ([#102](https://github.com/input-output-hk/atala-prism-mediator/issues/102)) ([de8b702](https://github.com/input-output-hk/atala-prism-mediator/commit/de8b702e6fa70a6a8f5ee311c7a236f45d328c04))

## [0.9.1](https://github.com/input-output-hk/atala-prism-mediator/compare/prism-mediator-v0.9.0...prism-mediator-v0.9.1) (2023-08-22)


### Bug Fixes

* collectionName messages.outbound ([#92](https://github.com/input-output-hk/atala-prism-mediator/issues/92)) ([a1bd657](https://github.com/input-output-hk/atala-prism-mediator/commit/a1bd657df0cc12a30bcc910028a84104efb9f65a))

# [0.9.0](https://github.com/input-output-hk/atala-prism-mediator/compare/prism-mediator-v0.8.2...prism-mediator-v0.9.0) (2023-08-21)


### Features

* Store outbound messages ([#84](https://github.com/input-output-hk/atala-prism-mediator/issues/84)) ([3576656](https://github.com/input-output-hk/atala-prism-mediator/commit/3576656844834d0ecc6365b912dda6f383936d5f))

## [0.8.2](https://github.com/input-output-hk/atala-prism-mediator/compare/prism-mediator-v0.8.1...prism-mediator-v0.8.2) (2023-08-18)


### Bug Fixes

* alias list is empty in new acounts by default ([#87](https://github.com/input-output-hk/atala-prism-mediator/issues/87)) ([39484e6](https://github.com/input-output-hk/atala-prism-mediator/commit/39484e68b87bf2c20570c271b9ee8fd447471b9f))

## [0.8.1](https://github.com/input-output-hk/atala-prism-mediator/compare/prism-mediator-v0.8.0...prism-mediator-v0.8.1) (2023-08-18)


### Bug Fixes

* reply condition check ReturnRoute ([#88](https://github.com/input-output-hk/atala-prism-mediator/issues/88)) ([68c86c8](https://github.com/input-output-hk/atala-prism-mediator/commit/68c86c871b8300e08cadbaf64284dc4d0e4abb3f))

# [0.8.0](https://github.com/input-output-hk/atala-prism-mediator/compare/prism-mediator-v0.7.0...prism-mediator-v0.8.0) (2023-08-18)


### Features

* reply asynchronous unless return_route all ([#86](https://github.com/input-output-hk/atala-prism-mediator/issues/86)) ([6249f37](https://github.com/input-output-hk/atala-prism-mediator/commit/6249f3701e2247614a5b42042cdbd8e0ab4541bb))

# [0.7.0](https://github.com/input-output-hk/atala-prism-mediator/compare/prism-mediator-v0.6.0...prism-mediator-v0.7.0) (2023-08-18)


### Bug Fixes

* mediator test ([#71](https://github.com/input-output-hk/atala-prism-mediator/issues/71)) ([7572dcc](https://github.com/input-output-hk/atala-prism-mediator/commit/7572dcc5bbd8ec07ee452bfc044863e50324c909))
* parsing error when return_route is none ([#77](https://github.com/input-output-hk/atala-prism-mediator/issues/77)) ([02dde1e](https://github.com/input-output-hk/atala-prism-mediator/commit/02dde1efc3b051b8c65e0d819c8737ebe769a66c))
* UserAccountRepo.createOrFindDidAccount ([#69](https://github.com/input-output-hk/atala-prism-mediator/issues/69)) ([3526f0a](https://github.com/input-output-hk/atala-prism-mediator/commit/3526f0a358b9928d74d1600b5705d42e36c90791))


### Features

* Error handling and Send Problem Reports ([#65](https://github.com/input-output-hk/atala-prism-mediator/issues/65)) ([fe46055](https://github.com/input-output-hk/atala-prism-mediator/commit/fe460550e8f1906eeaf29eb8cec45f6170fe7cbd))

# [0.6.0](https://github.com/input-output-hk/atala-prism-mediator/compare/prism-mediator-v0.5.0...prism-mediator-v0.6.0) (2023-07-27)


### Features

* add helm-chart for mediator ([#64](https://github.com/input-output-hk/atala-prism-mediator/issues/64)) ([52e5d3b](https://github.com/input-output-hk/atala-prism-mediator/commit/52e5d3bf031895336279d6981016bada9ce32eaf)), closes [#61](https://github.com/input-output-hk/atala-prism-mediator/issues/61) [#63](https://github.com/input-output-hk/atala-prism-mediator/issues/63)
* helm-chart appVersion bump and addtional fixes ([#67](https://github.com/input-output-hk/atala-prism-mediator/issues/67)) ([3ccbe3e](https://github.com/input-output-hk/atala-prism-mediator/commit/3ccbe3ed093c7f22dbd4b934e9d9ce8488cbd302))

# [0.5.0](https://github.com/input-output-hk/atala-prism-mediator/compare/prism-mediator-v0.4.1...prism-mediator-v0.5.0) (2023-07-21)


### Features

* add endpoint to get the OOB mediate invitation ([#63](https://github.com/input-output-hk/atala-prism-mediator/issues/63)) ([c82282c](https://github.com/input-output-hk/atala-prism-mediator/commit/c82282ca8c7061cc1ec702af538ab77e2c9a1f3c))

## [0.4.1](https://github.com/input-output-hk/atala-prism-mediator/compare/prism-mediator-v0.4.0...prism-mediator-v0.4.1) (2023-07-20)


### Bug Fixes

* mediator ATL-4883 pickup status message to sync reply ([#59](https://github.com/input-output-hk/atala-prism-mediator/issues/59)) ([c0b6de0](https://github.com/input-output-hk/atala-prism-mediator/commit/c0b6de0f4fe4702641ff9e8a371b3aff3cd74e1f))

# [0.4.0](https://github.com/input-output-hk/atala-prism-mediator/compare/prism-mediator-v0.3.1...prism-mediator-v0.4.0) (2023-07-13)


### Bug Fixes

* Id/hash of the message must be deterministic ([#53](https://github.com/input-output-hk/atala-prism-mediator/issues/53)) ([d122b99](https://github.com/input-output-hk/atala-prism-mediator/commit/d122b993d54b3a8e557374709b9d8628c38ee74e))


### Features

* Better error handling for connection refused ([#47](https://github.com/input-output-hk/atala-prism-mediator/issues/47)) ([429940e](https://github.com/input-output-hk/atala-prism-mediator/commit/429940e2ef6807017c4e4ef156432e843c5cdccc))
* Not send response errors to the caller ([#50](https://github.com/input-output-hk/atala-prism-mediator/issues/50)) ([60ee3ef](https://github.com/input-output-hk/atala-prism-mediator/commit/60ee3ef8e4342fb5fa69501502abdd739c55e22a))

## [0.3.1](https://github.com/input-output-hk/atala-prism-mediator/compare/prism-mediator-v0.3.0...prism-mediator-v0.3.1) (2023-07-10)


### Bug Fixes

* mediator oob webpage added logo  ([#42](https://github.com/input-output-hk/atala-prism-mediator/issues/42)) ([45debc8](https://github.com/input-output-hk/atala-prism-mediator/commit/45debc8c2d607cb298af0f1b047fb2083a334b71))

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
