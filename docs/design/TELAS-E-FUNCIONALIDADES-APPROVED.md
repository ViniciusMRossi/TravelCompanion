# Travel Companion — Descrição das telas e funcionalidades

Estado em 1 de setembro de 2026. Cobre o que está construído nos dois protótipos:

- `Travel Companion - Fluxo Principal.dc.html` — telas 01, 02, 06, 07, 08, 09, 14, 15 (fluxo aprovado)
- `Travel Companion - Telas Secundárias.dc.html` — telas 03, 04, 10, 11, 12, 13, 16, 17, 18 + pranchas de estado S1–S5

Viewport de referência: 412 × 915 (Android). Conteúdo de exemplo: Mochilão pelos Bálcãs 2026, 21 dias, 7 países, dia 9 em Sarajevo, participantes Vinícius e Érika.

---

## Princípios que governam todas as telas

**Android-first.** Alvos mínimos de 48dp, navegação inferior de 5 itens, voltar sempre no canto superior esquerdo, barra de gesto na base.

**Offline é o modo normal.** Nenhuma tela mostra erro de rede. Offline aparece como pílula neutra contextual ("Offline · conteúdo disponível"), nunca como banner de alarme. Só existe aviso quando a falta de internet afeta o conteúdo, e o aviso diz o que ainda funciona.

**Operacional versus editorial.** Informação operacional (horário, plataforma, localizador, telefone) usa Roboto, numerais tabulares, fundo claro e alto contraste. Conteúdo editorial (nome de cidade, atração, passeio, título de história) usa Fraunces e pode ocupar fotografia. Nenhum horário crítico vive dentro de bloco editorial.

**Sincronização é infraestrutura invisível.** O grupo aparece como pessoas e estados humanos ("Sincronizado", "Por perto", "Sincronizando novamente"). Nunca há IP, latência, protocolo, sala, código ou convite.

**Modo Passeio funciona no bolso.** Fundo escuro, instrução em 22px, controles de 56–76dp, nada que exija leitura fina em movimento.

---

# Fluxo principal

## 01 — Quem é você?

**Papel.** Primeira execução. Identifica qual participante está com este aparelho. Não é login: as duas pessoas já pertencem ao grupo da viagem.

**Conteúdo.** Fotografia de Stari Most no topo. Eyebrow operacional "21 dias · 7 países · setembro de 2026". Título Fraunces 40px "Quem é você?". Parágrafo explicando que roteiro, audioguias e documentos já estão no aparelho.

**Interação.** Duas linhas de 76dp, cada uma com avatar de inicial, nome em 18px e a responsabilidade da pessoa na viagem ("Reservas e transportes no nome dele" / "Seguro e documentos no nome dela"). Um toque grava o participante e leva direto para Hoje — sem confirmação, sem segunda etapa.

**Estados.** Rodapé informa que a viagem inteira está salva no aparelho e que dá para trocar de pessoa depois em Configurações. Não há estado de erro: a tela funciona sem internet.

**Sem.** Senha, e-mail, código de pareamento, criação de conta, convite.

---

## 02 — Hoje

**Papel.** Tela operacional mais forte do app. Responde, em ordem: o que é agora, o que não pode dar errado hoje, o que vestir, e o dia inteiro.

**Cabeçalho.** Cidade e país com ícone de pino, "Dia 9 de 21", pílula de offline. Data por extenso e nome da cidade em Fraunces 32px.

**Cartão Agora (ink).** Único bloco escuro da tela. Hora em 34px, atração em 24px, distância a pé, e o horário do próximo compromisso embutido na frase. Linha de audioguia com duração e "salvo no aparelho". Dois botões: *Ver atração* (leva a 03) e *Abrir no Maps*.

**Item crítico.** Faixa oxblood à esquerda, ícone de aviso, rótulo "Não pode dar errado", hora em 24px, e a instrução operacional destacada do horário nominal: o ônibus é 19:30, mas a instrução é estar na estação até 19:00. Dois botões de 52dp: *Mostrar passagem*, *Ver ponto de embarque*.

**Clima e o que vestir.** Dois cards lado a lado. Temperatura, mínima/máxima, e duas listas curtas — o que vestir, o que levar. Quando o app está offline, o rodapé do card diz "Sem internet · previsão de 08:14" com ícone, em vez de fingir que o dado é atual.

**Timeline do dia.** Oito itens de 08:00 a 22:30. Cada linha tem hora em numeral tabular, marcador, título e uma linha de detalhe operacional. Passado usa opacidade reduzida além da cor. O item atual tem marcador teal e pílula "Agora". O item crítico tem marcador oxblood. Status de reserva aparece como pílula à direita ("Reservado", "Comprar").

**Atalhos.** Passagem do dia (com "Offline"), Plano B do dia (fundo âmbar), Gravar memória.

**Navegação.** Barra inferior de 5 itens: Hoje, Viagem, Explorar, Carteira, Mais.

---

## 03 — Dia completo

**Papel.** O mesmo dia de Hoje, mas legível de ponta a ponta e navegável para outros dias.

**Cabeçalho.** Data, "Dia 9 de 21 · Sarajevo", voltar para Hoje, e setas de dia anterior/próximo de 48dp.

**Estrutura.** O item crítico do dia aparece primeiro, resumido, com os mesmos dois botões de Hoje — quem abre o dia completo às 18:00 não precisa rolar para achar o ônibus. Depois o dia é dividido em Manhã, Tarde e Noite, com a mesma timeline de Hoje.

**Blocos de fim de dia.** Card de transporte com origem/destino, horários em 28px, operadora e plataforma, botão *Ver transporte* (12). Card de hospedagem com check-in e botão *Ver hospedagem* (13).

**Rodapé.** Documentos deste dia (leva a 10) e Plano B deste dia (leva a 17).

---

## 04 — Cidade

**Papel.** Camada editorial da cidade. Contexto, não operação.

**Hero.** Fotografia de 300px, país, nome em Fraunces 40px, período da estadia. Voltar sobreposto de 48dp.

**Abertura editorial.** Parágrafo de 18px/27px em serifa-adjacente, sem números operacionais.

**Audioguia da cidade.** Card teal com play de 46dp, 5 capítulos, 34 min, "salvo offline".

**Capítulos.** Lista numerada em Fraunces com duração de cada capítulo à direita.

**Atrações.** Cards horizontais com miniatura, nome em Fraunces, horário do dia em que aparecem no roteiro, e pílulas de status ("Audioguia", "Offline", "Comprar", "10 KM").

**Passeios.** Card ink com o passeio guiado do dia: distância, duração, número de histórias.

**Onde comer.** Lista de três lugares com distância a pé e observação prática (só dinheiro, horário de abertura).

**Histórias curtas.** Cards editoriais com duração, título Fraunces, texto e dois botões — *Ouvir* e *Ler*.

---

## 05 — Atração

**Papel.** A ficha de um ponto específico, com a ponte para o passeio.

**Hero.** Fotografia, cidade e país, nome em Fraunces 30px, tipo e data de fundação.

**Pílulas.** Duração do audioguia, "Salvo offline", condição de entrada.

**Texto editorial.** Parágrafo de 18px/27px.

**Faixa operacional.** Bloco separado, claro: "11:00 · Caminhada Histórica sai daqui", ponto de encontro e número de histórias. O horário nunca fica dentro do parágrafo editorial.

**Ações.** *Ouvir audioguia · 12 min*, *Abrir no Maps*, *Como chegar*.

**O que observar.** Três itens numerados em dourado, curtos, de olhar e não de ler.

**Plano B.** Bloco âmbar com a alternativa se chover.

**Barra fixa.** *Iniciar passeio*, 56dp, teal.

---

## 06 — Iniciar passeio

**Papel.** Última tela antes de o celular ir para o bolso. Confirma que tudo que o passeio precisa está pronto.

**Card ink.** Passeio, horário de início, distância, duração, número de histórias, e a rota nomeada por pontos.

**Checklist de prontidão.** Três linhas: fones conectados ("Prontos"), histórias pelo caminho ("Ativado"), e uma explicação em texto corrido de que o app avisa ao chegar perto de um lugar interessante mesmo com a tela apagada, e que os áudios já estão no aparelho.

**Quem vai ouvir.** Duas pílulas de participante com avatar e ponto de estado: "você" e "por perto".

**Barra fixa.** *Começar passeio* e, abaixo, a frase que define o modo: "Pode guardar o celular no bolso. O passeio continua com a tela apagada."

---

## 07 — Passeio ativo

**Papel.** Modo Passeio. Desenhado para ser lido em dois segundos, de pé, na rua.

**Fundo ink**, único no app junto com 08 e a tela de história.

**Topo.** "3 de 7", "≈ 40 min restantes", e fechar de 48dp. Barra de sete segmentos mostrando progresso pelo passeio.

**Tocando agora.** Pílula com ponto pulsante, título da história em Fraunces 34px, e a posição na série.

**Instrução de caminho.** Bloco destacado, "Continue", texto em 22px/30px. É a informação mais legível da tela porque é a que se lê andando.

**Próxima história.** Nome e distância em metros.

**Estados discretos.** "Localização ativa" e "{outra pessoa} por perto", ambos como ponto colorido + palavra, sem jargão.

**Transporte de áudio.** Barra de progresso, tempo decorrido e total em numeral tabular, −15 / pausar / +15 com 56dp, 76dp e 56dp. *Ouvir juntos* como ação secundária de largura total.

**Andaime de protótipo.** Um botão tracejado rotulado "Protótipo ·" simula a chegada num ponto de história. Não faz parte do produto.

---

## 08 — Participantes sincronizados

**Papel.** Momento de transição de três segundos entre pedir "Ouvir juntos" e o áudio começar nos dois aparelhos.

**Conteúdo.** Título da história, dois avatares de 72dp com anel verde e a palavra "Sincronizado", contagem regressiva 3–2–1 em círculo de 120dp, e a frase "Os dois aparelhos vão tocar a mesma parte da história. Podem guardar o celular."

**O que não aparece.** Nome de rede, força de sinal, protocolo, papel de host ou cliente, tempo de latência. A sincronização se descreve pelo resultado.

---

## 09 — Ouvir juntos

**Papel.** Reprodução compartilhada em tela clara, para quando as pessoas estão paradas olhando o app.

**Hero.** Fotografia do ponto com o título em Fraunces.

**Player.** Card teal: capítulo, nome do passeio, barra de progresso, tempos, e transporte de 52/68/52dp.

**Lista de quem está ouvindo.** Cada participante com avatar, nome e estado. Dois estados cobertos:
- *Sincronizado* — ponto verde nos dois.
- *Sincronizando novamente* — ponto âmbar pulsante na outra pessoa, mais um aviso âmbar: "Não foi possível sincronizar o grupo agora. Seu audioguia continua funcionando normalmente." O playback local nunca para por causa do grupo.

**Ações.** *Transcrição* e *Próxima história*.

---

## 10 — História disparada pela localização

**Papel.** Interrupção editorial durante o passeio, provocada por chegar perto de um lugar.

**Composição.** O passeio em andamento continua visível no topo, esmaecido, com "4 de 7 · ≈ 28 min restantes". A folha editorial sobe do rodapé em fundo claro, criando a separação entre operação e narrativa.

**Conteúdo.** Pílula "História pelo caminho" com pino, título em Fraunces 32px, parágrafo de 18px/27px, e uma linha operacional separada com distância, local exato e duração do áudio.

**Ações.** *Ouvir agora* (56dp, teal), *Ler*, *Depois*. Rodapé explica que o aviso chegou como toque suave nos fones e que o celular pode continuar no bolso.

---

## 11 — Fim do passeio

**Papel.** Fechar o passeio e devolver a pessoa ao dia.

**Conteúdo.** Pílula "Passeio concluído", título Fraunces 36px, e a frase que diz onde e quando terminou. Quatro cards de resumo: duração, distância, histórias ouvidas, quem ouviu.

**Ainda hoje.** Bloco operacional com os dois compromissos restantes, incluindo o crítico em oxblood com a instrução de horário.

**Ações.** *Gravar memória* (teal, 56dp) e *Voltar para Hoje*.

---

## 12 — Gravar memória por voz

**Papel.** Registro de memória por voz, com local e hora automáticos. Voz é o formato primário; não há campo de texto.

**Contexto.** Linha com cidade, ponto e hora.

**Três estados no mesmo lugar:**

- *Parado* — botão de gravação de 82dp em oxblood, título "Registrar memória", e a promessa "O local e a hora entram sozinhos."
- *Gravando* — pílula pulsante "Gravando", cronômetro de 38px, oito barras de forma de onda animadas, atribuição (quem e onde), e *Pausar* / *Concluir* / *Cancelar*.
- *Salva* — pílula verde "Memória salva", confirmação com local, hora e duração, e volta para Hoje.

**Memórias desta viagem.** Lista de gravações anteriores com título, data, autor e duração.

---

# Telas de apoio

## 13 — Carteira

Todos os documentos da viagem, agrupados por urgência: **Hoje**, **Transporte da viagem**, **Seguro e documentos**. Cabeçalho traz a contagem total e a pílula "Tudo offline". Cada linha é um botão de 48dp com ícone, nome, detalhe operacional e status de reserva ("Emitido", "Pago", "Verificar"). Barra inferior com Carteira ativa.

## 14 — Documento / QR

**Modo ficha.** Bilhete desenhado como bilhete: horários de origem e destino em 28px, passageiros, plataforma, localizador em numeral tabular, valor. Perfuração tracejada separa o corpo do QR. Um recado explica que o motorista também aceita o localizador — redundância para quando o código não lê.

**Modo QR.** Tela branca de tela cheia, QR de 300px, localizador em 20px, resumo de uma linha, e a nota "Brilho no máximo · tela não apaga". Uma ação leva do documento ao modo QR; fechar volta.

## 15 — Transporte

Item crítico no topo com a instrução ("Esteja na estação até 19:00") separada do horário de partida, e os botões *Mostrar passagem* e *Ver ponto de embarque*. Abaixo, o trajeto vertical com horários de 28px, nome completo das estações, plataforma e distância a pé nas duas pontas. Rodapé de metadados: duração, pessoas, valor. Lista de ações: bilhete, como chegar, telefone da operadora. Fecha com o Plano B resumido e link para 17.

## 16 — Hospedagem

Hero com fotografia e nome em Fraunces, status "Pago". Check-in e check-out em dois cards. Item crítico próprio: a recepção fecha às 23:00, com a janela de ação ("ligue antes de 22:45"), o que acontece se não ligar, e botão *Ligar para o anfitrião*. Lista de ações: voucher offline, Maps, reserva original. Fecha com as instruções do anfitrião em texto corrido — portão, andar, café da manhã.

## 17 — Emergência

Tela de contraste elevado, sem barra inferior, sem hierarquia editorial. Localização atual em texto simples. Botão único de 88dp "Ligar 112" com a observação de que funciona sem crédito. Dois botões grandes para polícia e ambulância. Três contatos como botões de 48dp com ícone de telefone e rótulo de acessibilidade próprio: seguro (com número da apólice), hotel onde estão as malas, embaixada. Fecha com um cartão ink em bósnio para mostrar a um estranho, com a tradução abaixo.

## 18 — Plano B

Cenário nomeado em linguagem humana: "Se perderem o ônibus para Mostar", seguido da garantia de que ninguém fica na rua. Três passos numerados em ordem de tentativa — bilheteria, avisar a hospedagem, dormir em Sarajevo e pegar o trem das 06:39. Cada passo diz o horário-limite e o que esperar. Fecha com as alternativas já guardadas: trem, hostel perto da estação, telefone do anfitrião.

## 19 — Mais

Emergência em destaque no topo, oxblood, 68dp. Depois: **Na estrada** (frases úteis, apps, todos os Planos B), **Grupo** (os dois participantes com estado atual e a explicação de que o grupo se reencontra sozinho quando houver internet, sem que nada pare de funcionar), e **Viagem e aparelho** (informações da viagem, conteúdo salvo com o tamanho em disco, configurações — inclusive trocar quem é você).

---

# Pranchas de estado

Cada prancha mostra as variações lado a lado com o nome técnico do estado em mono, para handoff.

**S1 Conectividade** — online (atualizado agora), offline (pílula neutra), previsão desatualizada (âmbar, diz a hora do último dado), sincronização indisponível (âmbar, diz o que continua funcionando).

**S2 Participante** — sincronizado, conectando, sincronizando novamente (ponto pulsante), offline. Sempre ponto colorido + palavra; nunca só cor, nunca número.

**S3 Timeline** — concluído (opacidade + cinza), ativo (teal), futuro, crítico (oxblood). A distinção nunca depende só de matiz.

**S4 Áudio** — parado, tocando, pausado, reconectando. O estado de reconexão mantém a barra de progresso teal e afirma "Seu audioguia continua tocando": o áudio local nunca é refém do grupo.

**S5 Reserva** — reservado, verificar, comprar, e reservado + crítico. A criticidade é dimensão adicional sobre o status, não substituição: o cartão mostra as duas pílulas juntas.

---

# Convenções aplicadas

**Cores.** Ink #16232E, papel #F5F1E8, superfície #FFFDF8, teal #1F6F78, oxblood #7A2E2E, âmbar #B8863B / #F3E8D0, verde #4C6444 / #E7EDE3. Dois fundos no app inteiro: papel e ink.

**Tipografia.** Roboto para tudo operacional, Fraunces 600 para nomes próprios e títulos editoriais, Roboto Mono apenas em notas de handoff e legendas de placeholder. Numerais tabulares em todo horário, duração, valor e localizador.

**Forma.** Raio 14 em cards e botões, 20 em heros e blocos editoriais, 999 em pílulas e avatares. Sombra apenas no aparelho e no botão de gravação.

**Alvos.** 48dp mínimo em qualquer elemento tocável; 52–56dp em ações primárias; 76–88dp nos controles usados em movimento ou sob pressão.

**Nesting.** Nenhum card dentro de card. Blocos irmãos separados por gap, nunca por moldura dentro de moldura.

---

# Pendências

- Fotografias reais: todos os heros e miniaturas usam placeholder listrado com legenda em mono descrevendo a imagem esperada.
- QR real: os dois QRs são padrões gerados, não códigos válidos.
- Telas do índice ainda não construídas isoladamente: prancha-índice com miniaturas das 18 telas.
- Conteúdo além do dia 9: os outros 20 dias existem como referência textual, não como dados.
