package org.readium.r2.lcp

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.checkSuccess
import org.readium.r2.shared.util.resource.InMemoryResource
import org.readium.r2.shared.util.resource.Resource
import org.robolectric.ParameterizedRobolectricTestRunner

@OptIn(ExperimentalStdlibApi::class)
@RunWith(ParameterizedRobolectricTestRunner::class)
class LcpDecryptorTest(val testCase: TestCase) {

    data class TestCase(
        val range: LongRange,
        val paddingLength: Int
    )

    companion object {

        @ParameterizedRobolectricTestRunner.Parameters
        @JvmStatic
        fun testCases(): List<TestCase> = buildList {
            add(TestCase(0 until 16L, 1))
            add(TestCase(5 until 16L, 1))
            add(TestCase(5 until 18L, 1))
            add(TestCase(17 until 61L, 1))
            add(TestCase(45 until 55L, 1))
            add(TestCase(50 until 55L, 1))

            add(TestCase(0 until 16L, 5))
            add(TestCase(5 until 16L, 5))
            add(TestCase(5 until 18L, 5))
            add(TestCase(17 until 61L, 5))
            add(TestCase(45 until 55L, 5))
            add(TestCase(50 until 55L, 5))

            add(TestCase(0 until 16L, 16))
            add(TestCase(5 until 16L, 16))
            add(TestCase(5 until 18L, 16))
            add(TestCase(17 until 61L, 16))
            add(TestCase(45 until 55L, 16))
            add(TestCase(50 until 55L, 16))

            // Exhaustive testing, too long for CI
            /*for (padding in 1 until 16) {
                for (end in 0 until 60L) {
                    for (start in 0 until 100) {
                        add(
                            TestCase(
                                range = start until end,
                                paddingLength = padding
                            )
                        )
                    }
                }
            }*/
        }
    }

    // length = 48 bytes
    private val originalContent: String =
        "e7820056c8d9d4955270d7e0e06b85e5d4e0ae0c415d704a1e5035040b0aeb71955afdd796f8453d22ed41572e30ce39"

    // length = 16 bytes
    private val iv: String =
        "69d18631eb38909efc0835299e70b9e2"

    @Test
    fun `length and range decryption are ok`() = runTest {
        val slot = slot<ByteArray>()
        val license = mockk<LcpLicense>()

        coEvery { license.decrypt(capture(slot)) } answers {
            // Assume the encryption function is the identity and decrypt only removes IV
            Try.success(slot.captured.sliceArray(16 until slot.captured.size))
        }

        val padding =
            testCase.paddingLength.toByte().toHexString().repeat(testCase.paddingLength)

        val truncatedContent =
            originalContent.substring(0, originalContent.length - 2 * testCase.paddingLength)

        val encryptedContent: String =
            iv + truncatedContent + padding

        val encryptedResource = InMemoryResource(
            sourceUrl = null,
            properties = Resource.Properties(),
            bytes = { Try.success(encryptedContent.hexToByteArray()) }
        )

        val cbcResource = CbcLcpResource(
            resource = encryptedResource,
            originalLength = null, // we want to test length from padding
            license = license
        )

        val expectedEnd = (2 * (testCase.range.last + 1))
            .toInt()
            .coerceAtMost(truncatedContent.length)

        val expectedStart = (2 * testCase.range.first)
            .toInt()
            .coerceAtMost(expectedEnd)

        assertEquals(
            truncatedContent.length / 2,
            cbcResource.length().checkSuccess().toInt()
        )

        assertEquals(
            truncatedContent.substring(expectedStart, expectedEnd),
            cbcResource.read(testCase.range).checkSuccess().toHexString()
        )

        // Length computation follows a different path after a range request
        assertEquals(
            truncatedContent.length / 2,
            cbcResource.length().checkSuccess().toInt()
        )
    }
}
