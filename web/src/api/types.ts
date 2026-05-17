export interface PageResult<T> {
  records: T[];
  total: number;
  page: number;
  size: number;
}

export interface PageQuery {
  page?: number;
  size?: number;
}

