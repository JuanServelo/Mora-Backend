# Roles & Access Layers

## Layer 1 — Platform Owners (Us)

Developers and the company behind Mora. They need access to manage tenants (condominiums), monitor usage, handle billing, and provide support. They are never visible to end users.

| Profile | Function |
|---|---|
| Super Admin / Staff Mora | Manages all registered condominiums, plans, billing, and support |

---

## Layer 2 — Service Contractors (Tenant)

Two distinct profiles can contract Mora, each with different interests:

| Profile | Who They Are | Situation |
|---|---|---|
| Contracting Syndic | Syndic who contracts Mora for their own condominium | Self-managed condominium, no property management company |
| Contracting Property Manager | Property management company that contracts Mora to manage multiple condominiums under a single account | Portfolio of condominiums |

A property management company may manage 50 condominiums and would not need 50 separate logins. The tenant hierarchy can be:

Property Manager (1 SaaS account)  
├── Condominium A  
│ ├── Syndic A  
│ ├── Doormen  
│ └── Residents / Tenants  
├── Condominium B  
│ └── ...  
└── Condominium C  
└── ...


---

## Layer 3 — Condominium Users

Profiles: Syndic, Doorman, Resident Owner, Absent Owner, Lessee, Occupant, Real Estate Agency.

| Layer | Profile | Access Scope |
|---|---|---|
| Platform | Super Admin | All tenants, plans, support, global analytics |
| Tenant | Property Manager | 1 TO N condominiums within the same account |
| Condominium | Operational Syndic | Day-to-day condominium management |
| Condominium | Administrator | Financial + operational |
| Condominium | Doorman | Front desk and deliveries |
| Unit | Resident Owner | Full access + ownership voting |
| Unit | Absent Owner | Remote dashboard, contracts, financials |
| Unit | Lessee | Resident without ownership voting rights |
| Unit | Occupant (adult) | Basic resident features |
| Unit | Real Estate Agency | Proxy for the absent owner across their portfolio |

### Unit-Level Profile Summary

| Profile | One-liner | Can be financial resposible? |
|---|---|--|
| Real Estate Agency | "I represent the owner who isn't here" |idk|
| Resident Owner | "I own it and I live here" | yes |
| Absent Owner | "I own it but I don't live here" | no |
| Lessee | "I don't own it but I live here" | yes |
| Occupant | "I don't own it, I don't pay, but I live here" | no |
| Guest | "no acces to the sistem" | no |

1. resident owner é cadastrado pelo tenant maneger que o vincula a uma unidade
1. se o resident owner vende o ape, a desativacao dele e a ativacao do proximo resident owner tem que ser feita pelo tenant maneger
1. residente owner pode vincular somente a sua unidade (nem existe a posibilidade do contrario) occupants, lessees (locatario) e Guest (ex de como pode funcionar: ele adiciona o nome completo, cpf e email da pessoa, o sistema manda um email para a pessoa e quando o recebe para auth ela coloca todos os dados denovo, se nao bater as info nao consegue auth)
1. Se o Resident owner tem um lessee, o resident owner é o responsável financeiro (a parte de gerencia financeira entre resident/absent owner e lessee é uma outa parte do sistema/RF)
1. para um resident owner passar a responsabilidade financeira o perfil só pode ser um lessee e ao faze-lo ele automaticamente vira um absent onwer
1. se o absent owner tem um lessee o responsável financeiro é o lessee.
1. um absent owner só pode cadastar/vincular 1 lessee e mais ninguem
1. um lessee pode vincular occupants e Guest 
1. se um lessee é desativado, todos os occupant e nao profiles relacionando a ele tambem sao desativados
1. se um resident owner passa a responsabilidade financeira para outro alguem, todos que estavam vinculados a ele vao de vala tambem
1. Lessee esta sempre necessariamente vinculado a um owner
---

## Super Admin vs. Property Manager / Contracting Syndic

> **Decision rule:** "If I change this, does it affect only this condominium or all Mora clients?"

- Affects only this condominium → configuration belongs to the **Property Manager**
- Affects all clients or is infrastructure-level → configuration belongs to the **Super Admin**