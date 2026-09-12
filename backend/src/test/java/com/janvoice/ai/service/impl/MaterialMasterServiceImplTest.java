package com.janvoice.ai.service.impl;

import com.janvoice.ai.repository.MaterialMasterRepository;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MaterialMasterServiceImplTest {

    @Test
    void returnsEmptyListWhenDatabaseHasNoActiveMaterials() {
        MaterialMasterRepository repository = mock(MaterialMasterRepository.class);
        when(repository.findByActiveTrueOrderByCategoryAscCommonNameAsc())
                .thenReturn(Collections.emptyList());

        MaterialMasterServiceImpl service = new MaterialMasterServiceImpl(repository);

        assertTrue(service.findActive(null, null).isEmpty());
    }
}
