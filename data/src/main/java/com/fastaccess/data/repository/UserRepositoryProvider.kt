package com.fastaccess.data.repository

import androidx.lifecycle.LiveData
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.rx2.Rx2Apollo
import com.fastaccess.data.persistence.dao.UserDao
import com.fastaccess.data.persistence.models.UserCountModel
import com.fastaccess.data.persistence.models.UserModel
import com.fastaccess.data.persistence.models.UserOrganizationModel
import com.fastaccess.data.persistence.models.UserOrganizationNodesModel
import com.google.gson.Gson
import github.GetProfileQuery
import io.reactivex.Observable
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by Kosh on 10.06.18.
 */

class UserRepositoryProvider @Inject constructor(private val userDao: UserDao,
                                                 private val apolloClient: ApolloClient,
                                                 private val gson: Gson) : UserRepository {

    override fun getUserFromRemote(login: String): Observable<UserModel> = Rx2Apollo.from(apolloClient.query(GetProfileQuery(login)))
            .filter { !it.hasErrors() }
            .map { it ->
                Timber.e(gson.toJson(it.data()?.user))
                return@map it.data()?.user?.let { queryUser ->
                    UserModel(queryUser.databaseId
                            ?: 0, queryUser.login, queryUser.avatarUrl.toString(), queryUser.url.toString(), queryUser.name, queryUser.company,
                            queryUser.websiteUrl.toString(), queryUser.location, queryUser.email, queryUser.bio, queryUser.createdAt,
                            queryUser.createdAt, queryUser.isViewerCanFollow, queryUser.isViewerIsFollowing, queryUser.isViewer,
                            queryUser.isDeveloperProgramMember, UserCountModel(queryUser.followers.totalCount.toInt()),
                            UserCountModel(queryUser.following.totalCount.toInt()),
                            UserOrganizationModel(queryUser.organizations.totalCount.toInt(), queryUser.organizations.nodes?.map {
                                UserOrganizationNodesModel(it.avatarUrl.toString(), it.location, it.email, it.login, it.name)
                            }?.toList()))
                }?.apply {
                    userDao.upsert(this)
                }
            }

    override fun getUsers(): LiveData<List<UserModel>> = userDao.getUsers()
    override fun getUser(login: String): LiveData<UserModel> = userDao.getUser(login)
    override fun deleteAll() = userDao.deleteAll()
}

interface UserRepository {
    fun getUsers(): LiveData<List<UserModel>>
    fun getUser(login: String): LiveData<UserModel>
    fun getUserFromRemote(login: String): Observable<UserModel>
    fun deleteAll()
}
