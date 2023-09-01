package com.djm.gulimall.gulimallsearch.Service;

import com.djm.gulimall.gulimallsearch.vo.SearchParam;
import com.djm.gulimall.gulimallsearch.vo.SearchResult;

/**
 * @author djm
 * @create 2022-01-27 23:07
 */
public interface MallSearchService {
    SearchResult search(SearchParam param);
}
