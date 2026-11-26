# crUEL — Critérios e Resultados da Universidade Estadual de Londrina

Sistema web de **Avaliação Anônima de Disciplinas e Docentes** desenvolvido como projeto final da disciplina de **Banco de Dados I**.

## Identificação

* **Aluno(s):** `Kauã Felipe Martins`
* **Professor:** Prof. Dr. Anderson Paulo Avila Santos
* **Disciplina:** Banco de Dados I
* **Instituição:** Universidade Estadual de Londrina (UEL)

## Sobre o Projeto

O `Consulta e Relatório Unificado de Ensino e Lecionamento`, ou se preferir, `crUEL` (*"Feedback anônimo, forte e sem açúcar"*) tem como objetivo aplicar na prática os conceitos de modelagem de dados (DER/MER), normalização até a **3ª Forma Normal (3FN)**, DDL/DML em **PostgreSQL** e consultas SQL analíticas integradas a uma arquitetura em camadas.

### Natureza do Sistema e Particularidade (RF6)

* **Natureza:** Avaliação de Disciplinas ou de Docentes.
* **Regra de Negócio (Anonimato e Unicidade):** Cada aluno pode avaliar cada disciplina/oferta uma única vez por ano/semestre, mas suas respostas são **100% anônimas**. No banco de dados, o registro de quem já participou (`participacao_avaliacao`) é fisicamente separado da submissão das respostas (`submissao_anonima` e `resposta_questao`), sem qualquer vínculo de chave estrangeira com o usuário.

## Stack Tecnológica

* **Banco de Dados:** PostgreSQL
* **Back-end:** Java 21 + Spring Boot (acesso ao banco exclusivamente via **JDBC e classes DAO**, sem JPA/Hibernate)
* **Front-end:** React (Vite + Tailwind CSS + Recharts) com tema marrom escuro (*Espresso*) e alternância *Dark/Light*
