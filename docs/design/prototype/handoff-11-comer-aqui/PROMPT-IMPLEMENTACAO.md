# Prompt de implementação — Tela 11 "Comer aqui"

Tela nova. Uma página de sugestões de comida por dia da viagem: os pratos típicos da cidade onde o casal está naquele dia, com foto, descrição, história e a frase para pedir no idioma local. É conteúdo editorial dentro de um app offline-first, não um guia de restaurantes e não um agregador de avaliações.

## Arquivos do pacote

- `11 Comer aqui.dc.html` — referência executável. Abra no navegador. As pílulas abaixo do aparelho pulam para o dia 9 (Sarajevo), dia 10 (Mostar) e dia 3 (cidade sem cardápio escrito, para ver o estado de fallback).
- `tc-icons.js` — sprite SVG do Field Companion. Ícones usados aqui: `#tc-fork`, `#tc-arrow-left`, `#tc-chevron-left`, `#tc-chevron-right`, `#tc-chevron-right`, `#tc-offline`, `#tc-warning`, `#tc-sun`, `#tc-calendar`, `#tc-compass`, `#tc-wallet`, `#tc-more-horiz`.
- `support.js` — runtime da referência, não é parte do produto.

`#tc-fork` e `#tc-chevron-left` são ícones novos, adicionados ao sprite nesta entrega.

## Escopo desta entrega

1. Página de comida vinculada a um dia da viagem, alcançável a partir da tela Hoje.
2. Fichas de prato agrupadas por refeição, com foto, nome, pronúncia, preço, descrição, história e frase para pedir.
3. Navegação entre dias dentro da própria página.
4. Estado de fallback para dias cujo cardápio ainda não foi escrito.

Fora de escopo: busca, filtro por restrição alimentar, marcar prato como "já comi", lista de restaurantes com mapa, avaliações, reserva, tradução automática, foto tirada pelo usuário.

---

## 1 · Entrada e navegação

**Entrada.** Um item na lista de atalhos ao fim da tela Hoje (02), acima de "Gravar memória", com o mesmo formato dos vizinhos: ícone `#tc-fork` em `#1F6F78`, rótulo "Comer em {cidade}", contagem à direita ("10 pratos") e chevron. **A contagem é derivada do cardápio**, somando os pratos de todas as refeições — nunca um literal na tela. Altura mínima 56dp.

O rótulo carrega a cidade **do dia corrente da viagem**, não a palavra genérica "comida". São dois estados distintos e não devem compartilhar variável: o dia corrente é fixo (dia 9 hoje) e alimenta o atalho; o dia navegado é um cursor que existe só dentro da tela 11 e se reinicia no dia corrente cada vez que a tela abre. Se o cursor vazar para o atalho, a tela Hoje passa a anunciar a comida de outra cidade.

A contagem usa singular quando cabe ("1 prato"). Se a cidade não tem cardápio escrito, o atalho **não aparece** na tela Hoje — o fallback existe só para quem chega navegando entre dias, nunca como destino oferecido.

**Cabeçalho.** Três alvos de 48dp: voltar (`#tc-arrow-left`, seta cheia) à esquerda, e dia anterior / próximo dia (`#tc-chevron-left` / `#tc-chevron-right`, chevrons de 22dp) à direita. Duas formas diferentes de propósito: voltar sai da tela, os chevrons trocam o conteúdo dela. Ao centro, duas linhas: "Comer em {cidade}" em 16sp/500 e "Dia N de 21 · {dia da semana}, {data}" em 12sp `#65717A`.

O par de chevrons fica encostado; garanta que as áreas de toque não se sobreponham. Nos extremos (dia 1 e dia 21) o chevron correspondente fica **desabilitado**, não circula: mantém os 48dp de espaço, muda para `#B4BAB6`, perde o rótulo de acessibilidade e sai da ordem de foco. A referência mostra os dois estados — vá ao dia 1 pelas setas para ver o desabilitado.

**Barra inferior.** Item "Explorar" ativo em `#1F6F78`. Voltar e "Hoje" levam à tela 02.

## 2 · Cabeçalho editorial

Na ordem, antes das refeições:

- Eyebrow 12sp/700 caixa alta com `#tc-fork`: "Culinária local · {país}".
- Título Fraunces 600, 30sp/36. É um título por cidade, escrito à mão, não gerado ("Dez pratos da mesa bósnia"). Se o título cita um número, ele tem que bater com a contagem de pratos daquela cidade.
- Parágrafo de abertura 16sp/24 em `#4B5962`: o que caracteriza a cozinha daquele lugar, duas ou três frases.
- Linha de contexto 13sp com `#tc-offline`: moeda usada nos preços e o aviso de que a página fica offline. A moeda vem do dado da cidade, nunca fixa no layout.

## 3 · Ficha de prato

Card `#FFFDF8`, borda `#DDDCD4`, raio 18, sem sombra. Conteúdo na ordem:

**Foto** — 158dp de altura, largura total do card, sem raio próprio (o card corta). Na referência é um placeholder listrado com a legenda do que a foto deve mostrar. **Nenhuma foto real foi entregue**; ver "Ponto pendente".

**Nome e preço**, na mesma faixa:
- Nome em Fraunces 600, 24sp/30. É o nome local (`Ćevapi`, `Begova čorba`) com os diacríticos corretos — não translitere e não traduza.
- Pronúncia logo abaixo, Roboto Mono 13sp `#65717A`, entre barras (`/tchévapi/`). Aproximação para falante de português, não IPA.
- Faixa de preço em pílula `#E7EDE3` / texto `#3D5337`, numerais tabulares. É faixa observada, nunca preço exato.

**Descrição** 16sp/24: o que chega à mesa. Uma ou duas frases, concretas.

**História** 15sp/23 em `#4B5962`: origem, técnica ou o motivo de o prato ser daquele lugar. Um parágrafo. É a parte que dá razão à tela existir; se um prato não tem história, ele não entra na lista.

**Bloco "Para pedir"** — caixa `#E4F0F0` com borda `#C8DEDE`, raio 14: eyebrow "PARA PEDIR" em `#1F6F78`, a frase no idioma local em 17sp/500 e a tradução em 14sp `#3F6165`. O texto é selecionável e copiável. Não é um botão, não fala e não abre tradutor nesta entrega.

## 4 · Agrupamento por refeição

Um cabeçalho de seção por refeição, na ordem em que o dia acontece: **Café da manhã, Almoço, Jantar, Doce e café**. Fraunces 600, 22sp, com a faixa de horário à direita em 13sp `#65717A` (numerais tabulares) e linha divisória `#DDDCD4` abaixo.

Uma refeição sem prato escrito é **omitida**, não mostrada vazia. "Doce e café" usa "qualquer hora" no lugar do horário, e é intencional.

Rodapé da página, 13sp `#65717A`: quando os preços foram observados e a convenção de gorjeta.

## 5 · Dias sem cardápio

Nem toda cidade da viagem tem cardápio escrito. Quando não tem, a página abre com um aviso âmbar **acima do cabeçalho editorial**, primeiro elemento da rolagem — caixa `#F3E8D0`, borda `#E4D2AD`, ícone `#tc-warning` em `#684817`, texto `#5C4114`:

> Cardápio próprio de {cidade} ainda não escrito. Mostrando os pratos de Sarajevo como referência.

Abaixo dele, o conteúdo de Sarajevo com o **cabeçalho editorial de Sarajevo**: eyebrow "Culinária local · Bósnia e Herzegovina", título e abertura de Sarajevo, e a moeda de Sarajevo na linha de contexto. Nada no corpo da página assume o país ou a moeda do dia — só o cabeçalho do topo ("Comer em Ohrid", "Dia 3 de 21") e o próprio aviso mencionam a cidade real.

**A regra é a mesma do resto do app: nunca apresentar conteúdo de um lugar como se fosse de outro.** Herdar o país do trecho de rota em vez do cardápio exibido rotularia prato bósnio como culinária macedônia — é o erro exato que este estado existe para evitar. O aviso não é dispensável e não vira toast.

Cardápios escritos hoje: **Sarajevo** (dia 9, 10 pratos) e **Mostar** (dias 10–11, 7 pratos). Os outros 18 dias caem no fallback.

## 6 · Conteúdo e dados

O conteúdo é editorial e versionado com o app, não vem de API. Modele como recurso local por cidade:

```
Cidade { nome, país, moeda, título, abertura, refeições[] }
Refeição { nome, faixaHorário, pratos[] }
Prato { nome, pronúncia, faixaPreço, moeda, legendaFoto, foto, descrição, história, frase, tradução }
```

O dia da viagem mapeia para cidade por um trecho de rota (`dia inicial, dia final, cidade, país`), o mesmo mapeamento que a tela Viagem já usa. Não duplique essa tabela: leia a existente.

**País e moeda exibidos vêm sempre do registro Cidade do cardápio mostrado**, não do trecho de rota. A rota responde "que cidade é o dia N"; o cardápio responde "de onde é esta comida". No dia com cardápio próprio as duas respostas coincidem; no fallback, não, e é o cardápio que manda.

Todo texto é escrito em português do Brasil, exceto o nome do prato e a frase para pedir, que ficam no idioma local. Preços em moeda local com a sigla (KM, EUR, ALL, MKD, RSD, HRK→EUR), nunca convertidos para real.

---

## Regras que valem para tudo

- **Offline é o modo normal.** Texto e fotos são recursos locais. Nada nesta tela espera rede, incluindo a navegação entre dias.
- **Alvo mínimo 48dp** nos quatro botões do cabeçalho e nos itens da barra inferior.
- **Sem avaliação social.** Sem estrelas, sem "popular", sem contagem de curtidas, sem ranking de restaurantes.
- **Sem preço exato.** Sempre faixa, sempre com a data de observação no rodapé.
- **Sem tom de guia turístico.** A história do prato é informativa: origem, técnica, ingrediente, costume. Sem "imperdível", "autêntico", "experiência única", "não deixe de provar".
- **Diacríticos preservados** em nomes e frases (`č ć š ž đ`). Teste a fonte e o truncamento com eles.
- **Fraunces só em títulos** — título da página, nome da refeição, nome do prato. Todo o resto é Roboto.
- **Numerais tabulares** em preços e faixas de horário.
- **Sem card dentro de card.** O bloco "Para pedir" é filho do card do prato, separado por padding.
- **Rolagem vertical única.** Sem carrossel horizontal de pratos, sem abas por refeição — a página é lida de cima para baixo, na ordem do dia.

## Tokens

```
ink            #16232E
papel          #F5F1E8
superfície     #FFFDF8
borda          #DDDCD4   (borda de botão: #C8CCC5)
teal           #1F6F78   (caixa "Para pedir": #E4F0F0 / borda #C8DEDE / texto #3F6165)
âmbar aviso    #F3E8D0   (borda #E4D2AD · ícone #684817 · texto #5C4114)
verde preço    #E7EDE3   (texto #3D5337)
texto suave    #65717A   (corpo secundário: #4B5962)
placeholder    listras #D7CFBC / #CDC4AE a 115°
```

Raios: 18 no card do prato, 14 nas caixas internas, 999 na pílula de preço.

## Conteúdo da referência

**Sarajevo — 10 pratos.** Burek, kajmak sa somunom (café); ćevapi, begova čorba, klepe (almoço); bosanski lonac, sogan-dolma (jantar); tufahija, hurmašice, bosanska kafa (doce e café).

**Mostar — 7 pratos.** Uštipci (café); japrak, hercegovački pršut (almoço); pastrmka sa Neretve, jagnjetina ispod peke (jantar); smokvenjak, baklava (doce e café).

Todo esse texto é conteúdo aprovado, pronto para produção. Use verbatim.

## Ponto pendente para o design

**As fotos não existem.** Cada ficha tem uma legenda descrevendo o enquadramento pretendido ("dez ćevapi em somun com cebola crua, prato de metal"). São 17 fotos: 10 de Sarajevo e 7 de Mostar. Decidir se vêm de banco licenciado, de fotógrafo, ou das fotos da própria viagem — e nesse último caso a tela precisa de um estado para prato sem foto ainda, que não foi desenhado.

## Perguntas em aberto

- Quais cidades ganham cardápio escrito antes da viagem? O fallback funciona, mas 18 dias nele é muito.
- O bloco "Para pedir" deve ler em voz alta? Havendo síntese de fala offline no aparelho, é um botão de 48dp dentro da caixa — mas é entrega separada.
- Restrição alimentar (porco, vegetariano) foi cortada desta entrega. Se o produto quiser, é um selo por prato e muda a modelagem do conteúdo.
