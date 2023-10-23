package onlydust.com.marketplace.api.bootstrap.it;

import com.fasterxml.jackson.core.JsonProcessingException;
import onlydust.com.marketplace.api.bootstrap.helper.HasuraJwtHelper;
import onlydust.com.marketplace.api.postgres.adapter.entity.write.old.AuthUserEntity;
import onlydust.com.marketplace.api.postgres.adapter.entity.write.old.BudgetEntity;
import onlydust.com.marketplace.api.postgres.adapter.entity.write.old.CryptoUsdQuotesEntity;
import onlydust.com.marketplace.api.postgres.adapter.entity.write.old.ProjectToBudgetEntity;
import onlydust.com.marketplace.api.postgres.adapter.entity.write.old.type.CurrencyEnumEntity;
import onlydust.com.marketplace.api.postgres.adapter.repository.ProjectToBudgetIdRepository;
import onlydust.com.marketplace.api.postgres.adapter.repository.old.AuthUserRepository;
import onlydust.com.marketplace.api.postgres.adapter.repository.old.BudgetRepository;
import onlydust.com.marketplace.api.postgres.adapter.repository.old.CryptoUsdQuotesRepository;
import onlydust.com.marketplace.api.rest.api.adapter.authentication.hasura.HasuraJwtPayload;
import onlydust.com.marketplace.api.rest.api.adapter.authentication.jwt.JwtSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static onlydust.com.marketplace.api.rest.api.adapter.authentication.AuthenticationFilter.BEARER_PREFIX;

@ActiveProfiles({"hasura_auth"})
public class ProjectBudgetsApiIT extends AbstractMarketplaceApiIT {

    @Autowired
    AuthUserRepository authUserRepository;
    @Autowired
    JwtSecret jwtSecret;
    @Autowired
    ProjectToBudgetIdRepository projectToBudgetIdRepository;
    @Autowired
    BudgetRepository budgetRepository;
    @Autowired
    CryptoUsdQuotesRepository cryptoUsdQuotesRepository;

    @Test
    void should_return_forbidden_status_when_getting_project_budgets_given_user_not_project_lead() throws JsonProcessingException {
        // Given
        final AuthUserEntity pierre = authUserRepository.findByGithubUserId(16590657L).orElseThrow();
        final String jwt = HasuraJwtHelper.generateValidJwtFor(jwtSecret, HasuraJwtPayload.builder()
                .iss(jwtSecret.getIssuer())
                .claims(HasuraJwtPayload.HasuraClaims.builder()
                        .userId(pierre.getId())
                        .allowedRoles(List.of("me"))
                        .githubUserId(pierre.getGithubUserId())
                        .avatarUrl(pierre.getAvatarUrlAtSignup())
                        .login(pierre.getLoginAtSignup())
                        .build())
                .build());
        final UUID projectId = UUID.fromString("298a547f-ecb6-4ab2-8975-68f4e9bf7b39");

        // When
        client.get()
                .uri(getApiURI(String.format(PROJECTS_GET_BUDGETS, projectId)))
                .header("Authorization", BEARER_PREFIX + jwt)
                // Then
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.FORBIDDEN);
    }


    @Test
    void should_return_project_budgets_given_a_project_lead() throws JsonProcessingException {
        // Given
        final AuthUserEntity pierre = authUserRepository.findByGithubUserId(16590657L).orElseThrow();
        final String jwt = HasuraJwtHelper.generateValidJwtFor(jwtSecret, HasuraJwtPayload.builder()
                .iss(jwtSecret.getIssuer())
                .claims(HasuraJwtPayload.HasuraClaims.builder()
                        .userId(pierre.getId())
                        .allowedRoles(List.of("me"))
                        .githubUserId(pierre.getGithubUserId())
                        .avatarUrl(pierre.getAvatarUrlAtSignup())
                        .login(pierre.getLoginAtSignup())
                        .build())
                .build());
        final UUID projectId = UUID.fromString("f39b827f-df73-498c-8853-99bc3f562723");
        cryptoUsdQuotesRepository.save(CryptoUsdQuotesEntity.builder()
                .updatedAt(new Date())
                .price(BigDecimal.valueOf(1500))
                .currency(CurrencyEnumEntity.eth)
                .build());
        cryptoUsdQuotesRepository.save(CryptoUsdQuotesEntity.builder()
                .updatedAt(new Date())
                .price(BigDecimal.valueOf(120))
                .currency(CurrencyEnumEntity.apt)
                .build());
        final BudgetEntity budget1 = budgetRepository.save(BudgetEntity.builder()
                .id(UUID.randomUUID())
                .initialAmount(BigDecimal.valueOf(3000))
                .remainingAmount(BigDecimal.valueOf(100))
                .currency(CurrencyEnumEntity.stark)
                .build());
        final BudgetEntity budget2 = budgetRepository.save(BudgetEntity.builder()
                .id(UUID.randomUUID())
                .initialAmount(BigDecimal.valueOf(500))
                .remainingAmount(BigDecimal.valueOf(0))
                .currency(CurrencyEnumEntity.apt)
                .build());
        final BudgetEntity budget3 = budgetRepository.save(BudgetEntity.builder()
                .id(UUID.randomUUID())
                .initialAmount(BigDecimal.valueOf(200))
                .remainingAmount(BigDecimal.valueOf(50))
                .currency(CurrencyEnumEntity.eth)
                .build());
        projectToBudgetIdRepository.save(ProjectToBudgetEntity.builder()
                .id(ProjectToBudgetEntity.ProjectToBudgetIdEntity.builder()
                        .budgetId(budget1.getId())
                        .projectId(projectId)
                        .build())
                .build());
        projectToBudgetIdRepository.save(ProjectToBudgetEntity.builder()
                .id(ProjectToBudgetEntity.ProjectToBudgetIdEntity.builder()
                        .budgetId(budget2.getId())
                        .projectId(projectId)
                        .build())
                .build());
        projectToBudgetIdRepository.save(ProjectToBudgetEntity.builder()
                .id(ProjectToBudgetEntity.ProjectToBudgetIdEntity.builder()
                        .budgetId(budget3.getId())
                        .projectId(projectId)
                        .build())
                .build());

        // When
        client.get()
                .uri(getApiURI(String.format(PROJECTS_GET_BUDGETS, projectId)))
                .header("Authorization", BEARER_PREFIX + jwt)
                // Then
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody()
                .json("""
                        {
                            "initialDollarsEquivalent": 370000,
                            "remainingDollarsEquivalent": 79000,
                            "budgets": [
                                {
                                    "currency": "USD",
                                    "initialAmount": 10000,
                                    "remaining": 4000,
                                    "remainingDollarsEquivalent": 4000,
                                    "initialDollarsEquivalent": 10000
                                },
                                {
                                    "currency": "STARK",
                                    "initialAmount": 3000,
                                    "remaining": 100,
                                    "remainingDollarsEquivalent": null,
                                    "initialDollarsEquivalent": null
                                },
                                {
                                    "currency": "APT",
                                    "initialAmount": 500,
                                    "remaining": 0,
                                    "remainingDollarsEquivalent": 0,
                                    "initialDollarsEquivalent": 60000
                                },
                                {
                                    "currency": "ETH",
                                    "initialAmount": 200,
                                    "remaining": 50,
                                    "remainingDollarsEquivalent": 75000,
                                    "initialDollarsEquivalent": 300000
                                }
                            ]
                        }""");
    }
}