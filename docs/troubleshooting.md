# Troubleshooting

- On Node v17 you need use the legacy openssl:

  - Error: `error:0308010C:digital envelope routines::unsupported`
  - Solution:
    ```shell
    export NODE_OPTIONS=--openssl-legacy-provider
    ```
