package software.aws.toolkits.core.s3

import assertk.assert
import assertk.assertions.doesNotContain
import org.junit.Rule
import org.junit.Test
import software.amazon.awssdk.core.regions.Region
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus
import software.amazon.awssdk.services.s3.model.PutObjectRequest

class BucketUtilsTest {
    private val s3Client = S3Client.builder().region(Region.US_WEST_1).build()
    @Rule
    @JvmField
    val temporaryBucketRule = S3TemporaryBucketRule(s3Client)

    @Test
    fun deleteAnEmptyBucket() {
        createAndDeleteBucket {}
    }

    @Test
    fun deleteABucketWithObjects() {
        createAndDeleteBucket { bucket ->
            s3Client.putObject(PutObjectRequest.builder().bucket(bucket).key("hello").build(), RequestBody.of(""))
        }
    }

    @Test
    fun deleteABucketWithVersionedObjects() {
        createAndDeleteBucket { bucket ->
            s3Client.putBucketVersioning { it.bucket(bucket).versioningConfiguration { it.status(BucketVersioningStatus.ENABLED) } }
            s3Client.putObject(PutObjectRequest.builder().bucket(bucket).key("hello").build(), RequestBody.of(""))
            s3Client.putObject(PutObjectRequest.builder().bucket(bucket).key("hello").build(), RequestBody.of(""))
        }
    }

    private fun createAndDeleteBucket(populateBucket: (String) -> Unit) {
        val bucket = temporaryBucketRule.createBucket()
        populateBucket(bucket)
        s3Client.deleteBucketAndContents(bucket)
        assert(s3Client.listBuckets().buckets().map { it.name() }).doesNotContain(bucket)
    }
}