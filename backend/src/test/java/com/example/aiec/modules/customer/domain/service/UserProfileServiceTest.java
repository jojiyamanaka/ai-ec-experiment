package com.example.aiec.modules.customer.domain.service;

import com.example.aiec.modules.customer.domain.entity.User;
import com.example.aiec.modules.customer.domain.entity.UserAddress;
import com.example.aiec.modules.customer.domain.repository.UserAddressRepository;
import com.example.aiec.modules.customer.domain.repository.UserRepository;
import com.example.aiec.modules.shared.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    UserAddressRepository userAddressRepository;

    @InjectMocks
    UserProfileService userProfileService;

    @Test
    void addMyAddress_defaultAddress_shouldNormalizeOtherDefaults() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userAddressRepository.save(org.mockito.ArgumentMatchers.any(UserAddress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileService.AddressUpsertCommand command = new UserProfileService.AddressUpsertCommand();
        command.setRecipientName("田中 太郎");
        command.setPostalCode("1000001");
        command.setPrefecture("東京都");
        command.setCity("千代田区");
        command.setAddressLine1("千代田1-1");
        command.setIsDefault(true);

        userProfileService.addMyAddress(1L, command);

        verify(userAddressRepository).clearDefaultByUserId(eq(1L), isNull());
        ArgumentCaptor<UserAddress> captor = ArgumentCaptor.forClass(UserAddress.class);
        verify(userAddressRepository).save(captor.capture());
        assertThat(captor.getValue().getIsDefault()).isTrue();
    }

    @Test
    void updateMyAddress_otherUserAddress_shouldThrowForbidden() {
        User owner = new User();
        owner.setId(2L);

        UserAddress address = new UserAddress();
        address.setId(10L);
        address.setUser(owner);

        when(userAddressRepository.findById(10L)).thenReturn(Optional.of(address));

        UserProfileService.AddressUpsertCommand command = new UserProfileService.AddressUpsertCommand();
        command.setRecipientName("田中 花子");
        command.setPostalCode("1500001");
        command.setPrefecture("東京都");
        command.setCity("渋谷区");
        command.setAddressLine1("神南1-1");

        assertThatExceptionOfType(ForbiddenException.class)
                .isThrownBy(() -> userProfileService.updateMyAddress(1L, 10L, command))
                .satisfies(ex -> assertThat(ex.getCode()).isEqualTo("FORBIDDEN"));

        verify(userAddressRepository, never()).save(org.mockito.ArgumentMatchers.any(UserAddress.class));
    }
}
