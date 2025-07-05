# Passos para correr o sistema Googol com CLI client

## Preparação
1. Execute `make clean`
2. Execute `make`

## Execução
### Computador 1 (gateway+barrel+downloader)
3. Abra três terminais separados e execute os seguintes comandos:
    - `make run_gateway`
    - `make run_barrel`
    - `make run_downloader`

### Computador 2 (client+barrel+downloader)
4. Abra três terminais separados e execute os seguintes comandos:
    - `make run_client`
    - `make run_barrel`
    - `make run_downloader`

## Visualização dos Relatórios
5. Abra os arquivos `relatorio.md` e `manual_instalação.md` no VSCode.
6. Pressione `Ctrl+Shift+V` para visualizar os arquivos em modo de pré-visualização.

## Visualização do JavaDoc
7. Abra a pasta do projeto num terminal e execute o comando `xdg-open ./apidocs/index.html`


# Passos para correr o sistema Googol com WebServer

## Preparação
1. Execute `make clean`
2. Execute `make`

## Execução
### Computador 1 (gateway+barrels+downloaders)
3. Abra três terminais separados e execute os seguintes comandos:
    - `make run_gateway`
    - `make run_barrel`
    - `make run_downloader`
    - `make run_webserver`
    -`"http://localhost:8080"`

### Outros dispositivos
4. Se na mesma rede que o servidor, basta pesquisar o URL `http://<ipdoservidornarede>:8080`.
   Se presentes noutra rede, é necessário o servidor fazer um port forward no router da rede a que está ligado. Como alternativa é possível usar uma plataforma como o `ngrok` que permite criar um URL seguro para dispositivos noutras redes.


## Visualização dos Relatórios
5. Abra os arquivos `relatorio.md` e `README.md` no VSCode.
6. Pressione `Ctrl+Shift+V` para visualizar os arquivos em modo de pré-visualização.

## Visualização do JavaDoc
7. Abra a pasta do projeto num terminal e execute o comando `xdg-open ./apidocs/index.html`
