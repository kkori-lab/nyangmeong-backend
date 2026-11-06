package com.nyangmeong.global.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.naming.CommunicationException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;

import org.junit.jupiter.api.Test;

/**
 * [검사 - DNS 메일 도메인 확인]
 * - 네트워크 없이 조회 결과를 해석하는 규칙만 확인한다
 */
class DnsMailDomainCheckerTest {

    /** 조회 결과를 정해 둔 확인기 */
    private static DnsMailDomainChecker checker(List<String> mx, List<String> a, NamingException error) {
        return new DnsMailDomainChecker() {
            @Override
            protected List<String> lookup(String domain, String type) throws NamingException {
                if (error != null) {
                    throw error;
                }
                return "MX".equals(type) ? mx : a;
            }
        };
    }

    @Test
    void MX가_있으면_받을_수_있다() {
        assertThat(checker(List.of("10 mx.example.com."), List.of(), null).isReceivable("example.com")).isTrue();
    }

    @Test
    void null_MX만_있으면_받을_수_없다() {
        assertThat(checker(List.of("0 ."), List.of("1.2.3.4"), null).isReceivable("example.com")).isFalse();
    }

    @Test
    void null_MX와_일반_MX가_섞이면_받을_수_있다() {
        assertThat(checker(List.of("0 .", "10 mx.example.com."), List.of(), null).isReceivable("example.com"))
                .isTrue();
    }

    @Test
    void MX가_없고_A가_있으면_받을_수_있다() {
        assertThat(checker(List.of(), List.of("1.2.3.4"), null).isReceivable("example.com")).isTrue();
    }

    @Test
    void MX도_A도_없으면_받을_수_없다() {
        assertThat(checker(List.of(), List.of(), null).isReceivable("example.com")).isFalse();
    }

    @Test
    void 도메인이_없으면_받을_수_없다() {
        assertThat(checker(List.of(), List.of(), new NameNotFoundException()).isReceivable("naevr.com")).isFalse();
    }

    @Test
    void 조회가_실패하면_막지_않는다() {
        assertThat(checker(List.of(), List.of(), new CommunicationException()).isReceivable("example.com")).isTrue();
    }

    @Test
    void 그_도메인만_서버_오류이고_대조_도메인은_조회되면_받을_수_없다() {
        DnsMailDomainChecker checker = new DnsMailDomainChecker() {
            @Override
            protected List<String> lookup(String domain, String type) throws NamingException {
                if ("example.com".equals(domain)) {
                    return List.of("1.2.3.4");
                }
                throw new ServiceUnavailableException();
            }
        };

        assertThat(checker.isReceivable("naevr.com")).isFalse();
    }

    @Test
    void 대조_도메인도_서버_오류이면_우리_쪽_문제라_막지_않는다() {
        assertThat(checker(List.of(), List.of(), new ServiceUnavailableException()).isReceivable("naevr.com"))
                .isTrue();
    }

    @Test
    void 도메인이_비어_있으면_받을_수_없다() {
        assertThat(checker(List.of(), List.of(), null).isReceivable(" ")).isFalse();
    }

    @Test
    void 대소문자와_앞뒤_공백은_무시한다() {
        DnsMailDomainChecker checker = new DnsMailDomainChecker() {
            @Override
            protected List<String> lookup(String domain, String type) {
                return "example.com".equals(domain) ? List.of("10 mx.example.com.") : List.of();
            }
        };

        assertThat(checker.isReceivable(" Example.COM ")).isTrue();
    }
}
