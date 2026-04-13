package com.nhviewer.data.remote

import com.nhviewer.data.remote.dto.GalleryDetailDto
import com.nhviewer.data.remote.dto.GalleryDto
import com.nhviewer.data.remote.dto.GalleryListDto
import com.nhviewer.data.remote.dto.GalleryCommentDto
import com.nhviewer.data.remote.dto.FavoriteActionDto
import com.nhviewer.data.remote.dto.TagDto
import com.nhviewer.data.remote.dto.TagSearchRequestDto
import com.nhviewer.data.remote.dto.UserMeDto
import com.nhviewer.data.remote.dto.UserProfileDto
import retrofit2.http.DELETE
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface NhentaiService {
    @GET("/api/v2/galleries")
    suspend fun getAllGalleries(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 25
    ): GalleryListDto

    @GET("/api/v2/galleries/popular")
    suspend fun getPopular(
        @Query("page") page: Int? = null
    ): List<GalleryDto>

    @GET("/api/v2/search")
    suspend fun searchGalleries(
        @Query("query") query: String,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int = 1
    ): GalleryListDto

    @GET("/api/v2/galleries/tagged")
    suspend fun getTaggedGalleries(
        @Query("tag_id") tagId: Long,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 25
    ): GalleryListDto

    @GET("/api/v2/galleries/{gallery_id}")
    suspend fun getGalleryDetail(
        @Path("gallery_id") galleryId: Long,
        @Query("include") include: String? = null
    ): GalleryDetailDto

    @GET("/api/v2/galleries/{gallery_id}/comments")
    suspend fun getGalleryComments(
        @Path("gallery_id") galleryId: Long
    ): List<GalleryCommentDto>

    @GET("/api/v2/favorites")
    suspend fun getFavorites(
        @Query("page") page: Int = 1
    ): GalleryListDto

    @GET("/api/v2/galleries/{gallery_id}/favorite")
    suspend fun checkFavorite(
        @Path("gallery_id") galleryId: Long
    ): FavoriteActionDto

    @POST("/api/v2/galleries/{gallery_id}/favorite")
    suspend fun addFavorite(
        @Path("gallery_id") galleryId: Long
    ): FavoriteActionDto

    @DELETE("/api/v2/galleries/{gallery_id}/favorite")
    suspend fun removeFavorite(
        @Path("gallery_id") galleryId: Long
    ): FavoriteActionDto

    @GET("/api/v2/user")
    suspend fun getMe(): UserMeDto

    @GET("/api/v2/users/{user_id}/{slug}")
    suspend fun getUserProfile(
        @Path("user_id") userId: Long,
        @Path("slug") slug: String
    ): UserProfileDto

    @POST("/api/v2/tags/search")
    suspend fun searchTags(
        @Body request: TagSearchRequestDto
    ): List<TagDto>
}
