# Prompt de implementação — Tela 12 "Gravar memória por voz"

Entrega incremental. A tela já existia com três estados de gravação (parado, gravando, salva) e uma lista estática de memórias. **O que muda é só a lista**: cada memória passa a tocar, compartilhar e ser apagada. Não altere a gravação nem o layout acima da lista.

## Arquivos do pacote

- `12 Gravar memória.dc.html` — referência executável. Abra no navegador. As pílulas abaixo do aparelho alternam os estados de gravação e restauram a lista depois de apagar.
- `tc-icons.js` — sprite SVG do Field Companion. Ícones usados na lista: `#tc-play`, `#tc-pause`, `#tc-near-me`, `#tc-close`, `#tc-check`, `#tc-warning`.
- `support.js` — runtime da referência, não é parte do produto.

## Escopo desta entrega

1. Reproduzir uma memória gravada dentro do app.
2. Compartilhar a memória para fora do app (folha de compartilhamento do sistema).
3. Apagar a memória, com confirmação.

Fora de escopo: gravar, pausar gravação, transcrever, editar título, renomear, favoritar, reordenar.

---

## 1 · Reprodução

**Estrutura da linha.** O card da memória tem padding `8px 8px 8px 16px` e três alvos na mesma faixa:

- **Botão de reprodução** — ocupa toda a largura restante (`flex:1`), `min-height` 48dp, contém ícone + título + metadados. Tocar em qualquer parte do texto toca a memória. Não é um ícone isolado de 48dp: a área de toque é a linha.
- **Botão compartilhar** — 48 × 48dp, ícone `#tc-near-me`, cor `#1F6F78`.
- **Botão apagar** — 48 × 48dp, ícone `#tc-close`, cor `#7A2E2E`.

**Comportamento.**

- Ícone alterna `#tc-play` → `#tc-pause`; o `contentDescription` acompanha: "Tocar memória Ponte Latina" / "Pausar memória Ponte Latina".
- Enquanto toca, aparece **abaixo** da faixa de botões uma barra de progresso de 5dp (trilha `#E4F0F0`, preenchimento `#1F6F78`) com tempo decorrido e duração total em **numerais tabulares**, com recuo de 34dp à esquerda para alinhar com o título.
- Só uma memória toca por vez: iniciar uma para a outra.
- Ao terminar, a reprodução volta sozinha ao início e o ícone volta para play.
- Abrir qualquer folha (compartilhar ou apagar) pausa a reprodução.
- O áudio vem de arquivo local. Sem streaming, sem spinner, sem estado de rede.

**Nota de implementação da referência.** Na referência web, os dois ícones são ramos com `href` literal em vez de um valor interpolado — um `<use href>` preenchido em tempo de render dispara erro de recurso no console. É detalhe da referência, não requisito Android.

---

## 2 · Compartilhamento externo

O botão de compartilhar abre a **folha de compartilhamento do sistema Android** (`Intent.ACTION_SEND`, MIME de áudio, arquivo exposto via `FileProvider`). A folha na referência é uma **simulação** do que o sistema mostra; não a reimplemente como UI própria.

**Cabeçalho da folha (isso sim é nosso, se você usar `EXTRA_TITLE` / preview):** nome da memória e a linha "Arquivo de áudio · 00:48 · fica salvo no aparelho".

**Destinos mostrados na referência**, em ordem:

1. **Érika** — a outra pessoa da viagem. No Android real isto é um *direct share target* ou a primeira sugestão do sistema; se não for viável, o item sai e a folha começa nos apps.
2. **WhatsApp** — "Enviar como mensagem de áudio".
3. **Google Drive** — "Salvar na nuvem quando houver internet". O texto é deliberado: o app é offline-first e o envio pode ficar pendente. Não prometa upload imediato.
4. **Salvar no aparelho** — Downloads.

Os tiles com inicial (É, W, D, A) são placeholder. No produto, os ícones vêm do sistema; **não desenhe logos de marca**.

**Depois de escolher um destino,** a linha da memória exibe uma confirmação inline em verde `#3D5337` com `#tc-check`: "Enviada para WhatsApp". A confirmação é informativa e não bloqueia nada. Compartilhar **nunca** remove nem altera o arquivo local.

---

## 3 · Apagar com confirmação

O botão de apagar abre uma folha inferior modal — `role="dialog"`, `aria-modal="true"` (Android: `Dialog` / `ModalBottomSheet` com o foco preso dentro e o fundo inacessível ao leitor de tela).

**Conteúdo, na ordem:**

- Alça de 44 × 4dp centralizada.
- Eyebrow "APAGAR MEMÓRIA" em `#7A2E2E` com `#tc-warning`.
- Título 24sp/700: o nome da memória.
- Corpo 16sp: os metadados da gravação seguidos de "· a gravação sai deste aparelho e não pode ser recuperada."
- **Apagar** — 56dp, fundo `#7A2E2E`, texto branco.
- **Manter** — 56dp, contorno `#C8CCC5`, fundo `#FFFDF8`.

**Regras.**

- A ação destrutiva é nomeada ("Apagar"), nunca "OK". A saída é nomeada ("Manter"), nunca "Cancelar" — a pergunta é sobre conservar o arquivo.
- O botão destrutivo é o primeiro visualmente, mas a folha **abre com o foco no diálogo**, não no botão de apagar.
- Toque fora ou botão voltar equivale a **Manter**.
- Confirmar remove a linha da lista. Não há undo nesta entrega; se o produto quiser um snackbar de desfazer, isso muda a cópia do diálogo e precisa voltar para design.
- Se a lista ficar vazia, mostre o estado vazio: caixa de contorno tracejado `#C8CCC5`, texto centralizado 14sp `#65717A`, "Nenhuma memória gravada nesta viagem."
- Apagar uma memória já compartilhada apaga só a cópia local. Não tente recolher o que foi enviado, e não mencione isso na interface.

---

## Regras que valem para tudo

- **Alvo mínimo 48dp** em todos os três botões da linha. Eles ficam encostados; garanta que a área de toque não se sobreponha.
- **Offline é o modo normal.** Áudio é arquivo local. Nada nesta tela depende de conexão, incluindo apagar.
- **Sem terminologia técnica.** Sem "sincronizar", "upload", "peer", "buffer", "cache". O Drive fala em "quando houver internet".
- **Sem features sociais.** Compartilhar é entregar um arquivo ao sistema operacional. Não existe feed, curtida, comentário, link público ou perfil.
- **Estado nunca só por cor.** A confirmação de envio tem ícone + texto; a gravação em curso tem ponto pulsante + palavra.
- **Sem card dentro de card.** A barra de progresso e a confirmação são filhas do mesmo card da linha, separadas por padding, não por moldura nova.
- **Numerais tabulares** em cronômetro de gravação, tempo decorrido e duração.
- **Fraunces não aparece nesta tela.** Toda ela é operacional: Roboto.

## Tokens

```
ink            #16232E
papel          #F5F1E8
superfície     #FFFDF8
borda          #DDDCD4   (borda de botão: #C8CCC5)
teal           #1F6F78   (trilha de progresso: #E4F0F0)
oxblood        #7A2E2E   (fundo de aviso: #FDECEC)
verde          #4C6444   (texto de confirmação: #3D5337)
texto suave    #65717A
scrim          rgba(22,35,46,.55)
```

Raios: 14 em cards e botões, 20 no card de gravação, 28 no topo das folhas inferiores, 999 em pílulas e botões redondos.

## Conteúdo da referência

Duas memórias de exemplo, conteúdo real da viagem:

| Título | Metadados | Duração |
| --- | --- | --- |
| Ponte Latina | Hoje, 11:41 · Érika | 00:48 |
| Ksamil, última manhã | 14 de setembro · Vinícius | 01:22 |

## Ponto pendente para o design

O design system aprovado não tem ícone de compartilhar; a referência usa `#tc-near-me` como substituto. Se o app já tiver um ícone de compartilhar do sistema, use-o e nos avise para entrar no sprite.

## Perguntas em aberto

- O produto quer snackbar de desfazer depois de apagar? Se sim, a cópia do diálogo muda.
- "Érika" como destino direto na folha do sistema depende do que o app já registra como *direct share*. Diga o que existe hoje antes de implementar esse item.
