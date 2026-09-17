package com.documania.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DocumaniaBackendApplicationTests {

	@Test
	void applicationEntryPointExists() {
		assertDoesNotThrow(() ->
			Class.forName("com.documania.backend.DocumaniaBackendApplication")
		);
	}

}
